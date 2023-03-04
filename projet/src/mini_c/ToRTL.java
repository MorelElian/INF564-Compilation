package mini_c;

import java.util.HashMap;
// Les Eunop ne marchent pas
// Les comparaisons ne marchent pas : par exemple "if (x<8)"
// Je ne sais pas comment on traduit les Band et Bor, est-ce qu'on fait une �valuation paresseuse aussi ?
// Nous on n'a que des entiers, comment on �crit non(e) par exemple ? On fait (1-0) ou quoi ?
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
// Notes perso : 
// On a choisi la convention le callee change le next label ce qui n'est peut être pas le mieux ...

public class ToRTL extends EmptyVisitor
{
	RTLgraph graph;
	RTL current_instr; 
	Label current_label;
	Label next_label;
	Label exit_label;
	Register current_register;
	Register reg_struct = new Register();
	Register next_register;
	Register fun_result;
	RTLfun fun_to_add;
	int current_int ;
	HashMap<String,Register> local_variables = new HashMap<>();
	
	public RTLfile translate(File f)
	{
		RTLfile to_send = new RTLfile();
		
		for(Decl_fun current_funct : f.funs)
		{
			fun_to_add = new RTLfun(current_funct.fun_name);
			graph = new RTLgraph();
			exit_label = new Label();
			next_label = exit_label;
			current_label = exit_label;
			fun_result = new Register();
			
			int size_var = current_funct.fun_formals.size();
			for(int i = 0; i < size_var; i++)
			{
				current_funct.fun_formals.get(i).accept(this);
				fun_to_add.formals.add(current_register);
				// Pas sure parce que du coup on reconnait aussi les variables des autres foncitons en mettant les parametre dans local_variables...
			}
			
			current_funct.fun_body.accept(this);
			fun_to_add.exit = exit_label;
			fun_to_add.entry = current_label;
			fun_to_add.result = fun_result;
			fun_to_add.body = graph;
			to_send.funs.add(fun_to_add);
		}
		return to_send;
	}
	
	public void visit(Sblock n)
	{
		int size_dl_var = n.dl.size();
		for(int i = 0; i < size_dl_var; i++)
		{
			n.dl.get(i).accept(this);
		}
		int size = n.sl.size();
		for(int i = size-1; i > -1; i--)
		{
			n.sl.get(i).accept(this);
			next_label = current_label;
		}
	}
	
	public void visit(Sreturn n)
	{	
		//System.out.println("return");
		next_label = exit_label;
		current_register = fun_result;
		current_label = exit_label;
		n.e.accept(this);				
	}
	
	public void visit(Econst n)
	{
		
		next_label = current_label;
		current_int = n.i ;
		current_instr = new Rconst(n.i,current_register, next_label);
		current_label = graph.add(current_instr);
	}
	
	public void visit(Eunop expr) { // ne marche pas
		// Label label_memo = next_label ;
		// System.out.println("label_memo=" + label_memo);
		next_label = current_label ;
		//System.out.println(expr.u);
		 // on va chercher current_int
		
		switch(expr.u) {
			case Uneg :
			Register r2 = new Register();
			current_instr = new Rmbinop(Mbinop.Msub, r2, current_register, next_label);
			current_label = graph.add(current_instr);
			next_label = current_label;
			current_instr = new Rmbinop(Mbinop.Msub, r2, current_register, next_label);
			current_label = graph.add(current_instr);
			next_label = current_label;
			current_instr = new Rmbinop(Mbinop.Mmov, current_register,r2,next_label);
			current_label = graph.add(current_instr);
			next_label = current_label;
			expr.e.accept(this);
			
			break;
			
			case Unot :	
			Munop m = new Msetei(0) ;
			current_instr = new Rmunop(m,current_register,next_label);
			current_label = graph.add(current_instr);
			expr.e.accept(this);
			break;
		}

		// System.out.println("label_memo=" + label_memo);x
	}
	
	
	public void visit(Ebinop expr) {
		// Label label_memo = next_label ;
		Register r2 = current_register ;
		Register r1 = new Register() ;
		boolean flag = false;
		Mbinop m  = Mbinop.Madd;
		switch(expr.b) {
		case Badd : //done
			m = Mbinop.Madd ;
			break ;
		case Bsub :	//done
			m = Mbinop.Msub ;
			break ;
		case Bmul :	//done
			m = Mbinop.Mmul ;
			break;
		case Bdiv : //done
			m = Mbinop.Mdiv ;
			break ;
		case Band : 
			current_instr = new Rmunop(new Msetnei(0),current_register,current_label);
			current_label = graph.add(current_instr);
			this.RTLc(expr, current_label, current_label);
			flag = true;
			break ;
		case Bor : 
			current_instr = new Rmunop(new Msetnei(0),current_register,current_label);
			current_label = graph.add(current_instr);
			this.RTLc(expr, current_label, current_label);
			flag = true;
			break ;
		case Beq : //done
			m = Mbinop.Msete ;
			break ;
		case Bneq : //done
			m = Mbinop.Msetne ;
			break ;
		case Blt : //done
			m = Mbinop.Msetl ;
			break ;
		case Ble : //done
			m = Mbinop.Msetle ;
			break ;
		case Bgt : //done
			m = Mbinop.Msetg;
			break ;
		case Bge : //done
			m = Mbinop.Msetge ;
			break ;
		default :
			m = Mbinop.Mmul ;
			break ;
		}
		
		if(!flag)
		{
		current_instr = new Rmbinop(m, r1, r2, current_label);
		current_label = graph.add(current_instr);
		
		current_register = r1 ;
		next_label=current_label ;
		expr.e2.accept(this);
		next_label=current_label ;
		current_register = r2 ;
		expr.e1	.accept(this);
		}

	}
	
	public void visit(Decl_var var) { //on cree seulement un registre pour stocker la variable, et on s'en souvient dans local_variables
		current_register = new Register() ;
		//System.out.println(current_register + var.name);
		fun_to_add.locals.add(current_register);
		
		local_variables.put(var.name, current_register);
	} 
	
	
	public void visit(Eassign_local var) { //on a verifie dans le typer que la variable existe, on va juste aller la noter
		Register reg_of_access = current_register;
		current_register = local_variables.get(var.i) ;
		next_label = current_label ;
		
		
		
		current_instr = new Rmbinop(Mbinop.Mmov,reg_of_access,current_register,next_label);
		current_register = reg_of_access;
		current_label = graph.add(current_instr);
		var.e.accept(this);
	}
	
	public void visit(Eaccess_local var) {//attention, on ne verifie pas que la variable ait ete definie, mais deja fait dans typer non ?
		next_label = current_label;
		
		current_instr = new Rmbinop(Mbinop.Mmov, local_variables.get(var.i), current_register, next_label );
		current_label = graph.add(current_instr);
	}
	
	public void visit(Sskip s) {
		;
	}
	
	public void visit(Sexpr s) {
		current_register = new Register() ;
	
		s.e.accept(this);
		
	}
	
	public void visit(Sif s) {
		//attention, on ne distingue pas ce qu'il y a dans l'expression ici
		
		Label ending_label = current_label;
		s.s2.accept(this);
		Label label_false = current_label ;
		current_label = ending_label;
		s.s1.accept(this);
		Label label_true = current_label;
		this.RTLc(s.e, label_true, label_false); // on met l'instruction dans current_instr
	}
	
	public void visit(Swhile s) {
		
		Label label_false = current_label ;	
		current_label = graph.add(new Rgoto(current_label)); // il faudra modifier le label quand on saura lequel c'est
		Label label_retour = current_label ;
		s.s.accept(this);
		Label label_true = current_label;	
		this.RTLc(s.e, label_true, label_false); // on met l'instruction dans current_instr
		// Il faut mettre � jour le label de retour de Rgoto si l'expression est vraie
		graph.graph.put(label_retour, new Rgoto(current_label));
	}
	
	public void RTLc_binop(Ebinop e, Label truel, Label falsel) {
		// on traduit l expression en RTl et donne le controle a truel si elle est vraie, a falsel sinon
		// attention, on regarde juste si l'expression vaut pas 0, sinon on va autre part...
		switch(e.b) {
		case Band :
			
			this.RTLc(e.e2, truel, falsel) ; 
			this.RTLc(e.e1, current_label, falsel) ;
			break;
		case Bor :
			this.RTLc(e.e2, truel, falsel) ;
			this.RTLc(e.e1, truel, current_label ) ;
			break;
		default :
			Mubranch m = new Mjnz();
			current_instr = new Rmubranch(m, current_register, truel, falsel) ;
			current_label = graph.add(current_instr);
			next_label = current_label;
			e.accept(this);
			break;
		}
	}
	
	public void RTLc(Expr e, Label truel, Label falsel) {
		// je sais pas si du coup on est s�rs que e n'est pas un binop
		// on traduit l expression en RTl et donne le controle a truel si elle est vraie, a falsel sinon
		// attention, on regarde juste si l'expression vaut pas 0, sinon on va autre part...

		if (e instanceof Ebinop) this.RTLc_binop((Ebinop)e, truel, falsel);
		else {
			Mubranch m = new Mjnz() ;
			current_instr = new Rmubranch(m, current_register, truel, falsel) ;
			current_label = graph.add(current_instr);
			next_label = current_label;
			e.accept(this);
		}
	}
	
	public void visit(Ecall f) {
		// Register de sortie r_result vaut current_register
		// La liste des registres de parametres est d�j� d�finie ?
		
		int size_dl_par = f.el.size(); // le nombre de param�tre qu'on va appeler
		Register r_result = current_register ; // on renvoie le r�sultat dans le registre o� on est actuellement
		LinkedList<Register> rl = new LinkedList<Register>(); // rl contiendra les registres o� seront stock�es les variables (alors que dans la d�claration d'une fonction on a cr�� �a nous m�me ?
		for(int i = size_dl_par-1; i > -1; i--)
		{	
			current_register= new Register();
			
			// f.el.get(i).accept(this);
			rl.add(current_register);
		}	
//		System.out.println("variables d�clar�es pour la fonction " + f.i + " : " + rl);
		current_instr = new Rcall(r_result, f.i, rl, current_label);
		current_label = graph.add(current_instr);
		next_label = current_label;
		for(int i = size_dl_par-1; i > -1; i--)
		{	
			current_register = rl.get(i);
			
			f.el.get(i).accept(this);
		}	
		
	}
	
	public void visit(Eassign_field n)
	{	
		// il va y avoir du R Store
		
		next_label = current_label;
		int current_position = n.f.field_position;		
		Register r_e1 = new Register();
		current_instr = new Rstore(current_register,r_e1,current_position,next_label);
		current_label = graph.add(current_instr);
		n.e2.accept(this);
		reg_struct = r_e1;
		// il faut maintenant réussir à récuperer dans un registre l'adresse de la structure
		n.e1.accept(this);
		// il nous faut maintenant trouver la position du field dans la structure
		
		
		
		
	}
	public void visit(Eaccess_field n)
	{
	
		next_label = current_label;
		int current_position = n.f.field_position;
		current_instr = new Rload(reg_struct,current_position,current_register,next_label);
		current_label = graph.add(current_instr);
		current_register = reg_struct;
		n.e.accept(this);
		
		
		
	}
	public void visit(Esizeof n)
	{
		next_label = current_label;
		current_instr = new Rconst(n.s.struct_size,current_register,next_label);
		current_label = graph.add(current_instr);
	}
	
	
	
	
}