package mini_c;

import java.util.HashMap;
// Les Eunop ne marchent pas
// Les comparaisons ne marchent pas : par exemple "if (x<8)"
// Je ne sais pas comment on traduit les Band et Bor, est-ce qu'on fait une �valuation paresseuse aussi ?
// Nous on n'a que des entiers, comment on �crit non(e) par exemple ? On fait (1-0) ou quoi ?
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
// Notes perso : 
// On a choisi la convention le callee change le next label ce qui n'est peut être pas le mieux ...

public class ToERTL extends EmptyRTLVisitor
{
	ERTLfun fun_to_add;
	Label current_label;
	Label next_label;
	Label exit_label;
	Register fun_result;
	ERTLgraph graph;
	RTLgraph Rgraph;
	LinkedList<Register> callee_saved_correspondance = new LinkedList<>();
	public ERTLfile translate(RTLfile f)
	{
		ERTLfile to_send = new ERTLfile();
		
		for(RTLfun current_funct : f.funs)
		{
			fun_to_add = new ERTLfun(current_funct.name,current_funct.formals.size());
			graph = new ERTLgraph();
			
			
			current_funct.accept(this);
			
			
			
			fun_to_add.body = graph;
			Liveness Live = new Liveness(graph);
			//Live.print(fun_to_add.entry);
			Interference interference = new Interference(Live);
			interference.print();
			Coloring colors = new Coloring(interference);
			colors.print();
			to_send.funs.add(fun_to_add);
		}
		
		return to_send;
	}
	public void visit(RTLfun f)
	{
		current_label = new Label();
		fun_to_add.entry = current_label;
		// On va rajouter la les Instructions sur les ...
		
		
		allocate_frame();
		save_callee_saved();
		get_formals(f);
		fun_to_add.locals.addAll(f.locals);
		
		graph.put(current_label,new ERgoto(f.entry));
		current_label = f.entry;
		
		for (Map.Entry<Label, RTL> entry : f.body.graph.entrySet()) {
		    current_label = entry.getKey();
		    entry.getValue().accept(this);
		}
		
		current_label = f.exit;
		move_result(f.result);
		restore_callee_saved();
		deallocate_frame();
		graph.put(current_label,new ERreturn());
	}
	public void visit(Rconst R)
	{
		graph.put(current_label, new ERconst(R.i,R.r,R.l));
		
		
	}
	public void visit(Rload R)
	{
		graph.put(current_label, new ERload(R.r1,R.i,R.r2,R.l));
	}
	public void visit(Rstore R)
	{
		graph.put(current_label, new ERstore(R.r1,R.r2,R.i,R.l));
		
	
	}
	public void visit(Rmunop R)
	{
		graph.put(current_label, new ERmunop(R.m,R.r,R.l));
		
		
	}
	public void visit(Rmbinop R)
	{
		graph.put(current_label, new ERmbinop(R.m,R.r1,R.r2,R.l));
		
	}
	public void visit(Rmubranch R)
	{
		graph.put(current_label, new ERmubranch(R.m,R.r,R.l1,R.l2));
		
	}
	public void visit(Rmbbranch R)
	{
		graph.put(current_label, new ERmbbranch(R.m,R.r1,R.r2,R.l1,R.l2));
		
	}
	public void visit(Rgoto R)
	{
		graph.put(current_label, new ERgoto(R.l));
		
	}
	public void visit(Rcall R)
	{
		// Passage des arguments 
		
		for(int i = 0 ; i < R.rl.size(); i++)
		{
			Label next_label = new Label();
			if (i < Register.parameters.size())
			{
				graph.put(current_label,new ERmbinop(Mbinop.Mmov,R.rl.get(i),Register.parameters.get(i),next_label));
				current_label = next_label;
			}
			else
			{
				graph.put(current_label,new ERpush_param(R.rl.get(i),next_label));
				current_label = next_label;
			}
		}
		Label label_bifurq = new Label();
		graph.put(current_label,new ERcall(R.s,Math.min(R.rl.size(),Register.parameters.size()),label_bifurq) );
		

		if( R.rl.size()> Register.parameters.size())
		{
			current_label = graph.add(new ERmunop(new Maddi(8 * (R.rl.size() - Register.parameters.size())),Register.rsp,R.l));
			graph.put(label_bifurq,new ERmbinop(Mbinop.Mmov,Register.result,R.r,current_label));
		}
		
		else
		{
			graph.put(label_bifurq, new ERmbinop(Mbinop.Mmov,Register.result,R.r,R.l));
		}
		
		
	}
	
	public void allocate_frame()
	{
		next_label = new Label();
		graph.put(current_label, new ERalloc_frame(next_label));
		current_label = next_label;
	}
	public void save_callee_saved()
	{
		callee_saved_correspondance = new LinkedList<>();
		for(Register callee_saved : Register.callee_saved)
		{
			next_label = new Label();
			Register fresh_register = new Register();
			graph.put(current_label, new ERmbinop(Mbinop.Mmov,callee_saved,fresh_register,next_label));
			callee_saved_correspondance.add(fresh_register);
			fun_to_add.locals.add(fresh_register);
			current_label = next_label;
			
		}
	}
	public void get_formals(RTLfun current_funct)
	{
		for(int i = 0; i < current_funct.formals.size();i++)
		{
			next_label = new Label();
			if(i < Register.parameters.size())
			{
				graph.put(current_label,new ERmbinop(Mbinop.Mmov,Register.parameters.get(i),current_funct.formals.get(i),next_label));
				
			}
			else // Si il y a plus de 6 parametres il faut trouver le paramètre sur la pile
			{
				graph.put(current_label, new ERget_param(8 * (i- Register.parameters.size()),current_funct.formals.get(i),next_label));
			}
			current_label = next_label;
		}
	}
	
	public void move_result(Register result)
	{
		next_label = new Label();
		graph.put(current_label,new ERmbinop(Mbinop.Mmov,result,Register.result,next_label));
		current_label = next_label;
	}
	public void restore_callee_saved()
	{

		for(int i = 0 ; i < Register.callee_saved.size(); i++)
		{
			next_label = new Label();
			graph.put(current_label, new ERmbinop(Mbinop.Mmov,callee_saved_correspondance.get(i),Register.callee_saved.get(i),next_label));
			current_label = next_label;
		}
	}
	public void deallocate_frame()
	{
		next_label = new Label();
		graph.put(current_label, new ERdelete_frame(next_label));
		current_label = next_label;
	}
	
}
	