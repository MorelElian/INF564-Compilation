package mini_c;

import java.util.Map;

public class toLTL implements ERTLVisitor{
	private Coloring coloring; // coloriage de la fonction en cours de traduction
	int size_locals; // taille pour les variables locales
	LTLgraph graph;  // graphe en cours de construction
	LTLfun fun_to_add;
	LTL current_instr;
	Label current_label;
	Label next_label;
	Reg current_reg1;
	Reg current_reg2;
	public LTLfile translate(ERTLfile f)
	{
		LTLfile to_send = new LTLfile();
		for(ERTLfun current_funct : f.funs)
		{
			fun_to_add = new LTLfun(current_funct.name);
			graph = new LTLgraph();
			
			
			current_funct.accept(this);
			
			
			
			fun_to_add.body = graph;
			Liveness Live = new Liveness(current_funct.body);
			//Live.print(fun_to_add.entry);
			Interference interference = new Interference(Live);
			
			coloring = new Coloring(interference);
			
			to_send.funs.add(fun_to_add);
		}
		
		return to_send;
		
	}
	public void visit(ERTLfun f)
	{
		fun_to_add.entry = f.entry;
		for (Map.Entry<Label, ERTL> entry : f.body.graph.entrySet()) {
		    current_label = entry.getKey();
		    entry.getValue().accept(this);
		}
	}
	public void visit(ERconst n)
	{
		current_instr = new Lconst(n.i,coloring.colors.get(n.r),n.l);
		graph.put(current_label,current_instr);
	}
	public void visit(ERload n)
	{
		Operand r1 = coloring.colors.get(n.r1);
		Operand r2 = coloring.colors.get(n.r2);
		next_label = current_label;
		check_operand1(r1);
		current_label =next_label;
		next_label = n.l; //check_operand2 will possibly change the next_label in order deviate the CFG
		check_operand2(r2);
		current_instr = new Lload(current_reg1.r,n.i,current_reg2.r,next_label);
		graph.put(current_label, current_instr);
	}
	public void visit(ERstore n)
	{
		Operand r1 = coloring.colors.get(n.r1);
		Operand r2 = coloring.colors.get(n.r2);
		check_operand1(r1);
		
		next_label = n.l; //check_operand2 will possibly change the next_label in order deviate the CFG
		check_operand2(r2);
		current_instr = new Lstore(current_reg1.r,current_reg2.r,n.i,next_label);
		graph.put(current_label, current_instr);
	}
	public void visit(ERgoto n)
	{
		current_instr = new Lgoto(n.l);
		graph.put(current_label, current_instr);
	}
	public void visit(ERreturn n)
	{
		current_instr = new Lreturn();
	}
	public void visit(ERmunop n)
	{
		current_instr = new Lmunop(n.m,coloring.colors.get(n.r),n.l);
		graph.put(current_label, current_instr);
	}
	public void visit(ERmbinop n)
	{
		Operand r1 = coloring.colors.get(n.r1);
		Operand r2 = coloring.colors.get(n.r2);
		check_operand1(r1); // Par convention on fait le choix de toujours mettre r1 dans un tmp : comme cela le résultat est directement stocké dans le bon registre
		if(n.m  == Mbinop.Mmov && n.r1.equals(n.r2))
		{
			current_instr = new Lgoto(n.l);
		}
		else if(n.m == Mbinop.Mmul)
		{
			next_label = n.l;
			check_operand2(r2);
			current_instr = new Lmbinop(n.m,current_reg1,current_reg2,n.l);
			graph.put(current_label, current_instr);
			
		}
		else
		{
			current_instr = new Lmbinop(n.m,current_reg1,r2,n.l);
		}
	}
	public void visit(ERmubranch n)
	{
		current_instr = new Lmubranch(n.m,coloring.colors.get(n.r),n.l1,n.l2);
		graph.put(current_label, current_instr);
	}
	public void visit(ERmbbranch n)
	{
		current_instr = new Lmbbranch(n.m,coloring.colors.get(n.r1),coloring.colors.get(n.r2),n.l1,n.l2);
		graph.put(current_label, current_instr);
		
	}
	public void visit(ERpush_param n)
	{
		current_instr = new Lpush(coloring.colors.get(n.r),n.l);
		
	}
	public void visit(ERcall n)
	{
		current_instr = new Lcall(n.s,n.l);
	}
	public void check_operand1(Operand r1)
	{
		if(r1 instanceof Spilled)
		{
			next_label = new Label();
			current_reg1 = new Reg(Register.tmp1);
			current_instr = new Lmbinop(Mbinop.Mmov,r1,current_reg1,next_label);
			graph.put(current_label, current_instr);
			current_label = next_label;
			
			
			
		}
		else
		{
			current_reg1 = (Reg) r1;
		}
		
	}
	public void check_operand2(Operand r2)
	{
		if(r2 instanceof Spilled)
		{
			current_reg2 = new Reg(Register.tmp2);
			Label transfer = next_label; // next_label is n.l 
			next_label = new Label();
			graph.put(next_label, new Lmbinop(Mbinop.Mmov, current_reg2,r2,transfer));
			
		}
		else
		{
			current_reg2 = (Reg) r2;
		}
	}
	
	
}
