package mini_c;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
// les tructures sont elles declares en dehors de fonctions ? 
// la recursion ?
// deux fonctions qui sapellent pareil ca marche ?
// Dans les calls de fonctions, comment on gere les nulls ?
public class Typing implements Pvisitor {

	// le résultat du typage sera mis dans cette variable
	private File file;
	// et renvoyé par cette fonction
	File getFile() {
		if (file == null)
			throw new Error("typing not yet done!");
		return file;
	}
	LinkedList<Decl_fun> funs = new LinkedList<Decl_fun>();
	Typ fun_type;
	Typ current_type;
	Sblock bloc;
	Stmt current_stmt;
	Expr current_expr;
	String current_id;
	Field field_to_update;
	LinkedList<HashMap<String,Typ>> bloc_variableHT = new LinkedList<>();
	HashMap<String, Structure> structHT = new HashMap<>();
	HashMap <String, Decl_fun> functionsHT = new HashMap<>();
	// we define here the functions putchar and malloc and add them into functionsHT
	
	
	@Override
	public void visit(Pfile n) {
		// TODO Auto-generated method stub
		
		LinkedList<Decl_var> putchar_var = new LinkedList<Decl_var>();
		Decl_var x = new Decl_var (new Tint(),"c");
		putchar_var.add(x);
		Decl_fun putchar = new Decl_fun(new Tint(),"putchar",putchar_var,null);
		functionsHT.put("putchar",putchar);
		LinkedList<Decl_var> malloc_var = new LinkedList<>();
		malloc_var.add(new Decl_var(new Tint(),"n"));
		functionsHT.put("malloc", new Decl_fun(new Tvoidstar(),"malloc",malloc_var,null));
		for(Pdecl n_decl : n.l)
		{
			n_decl.accept(this);
		}
		if(!functionsHT.containsKey("main"))
		{
			throw new Error("Missing Entry point (function main)");
		}
		file = new File(funs);
		
	}

	@Override
	public void visit(PTint n) {
		// TODO Auto-generated method stub
		current_type = new Tint();
		
	}

	@Override
	public void visit(PTstruct n) {
		// TODO Auto-generated method stub
		if(structHT.get(n.id) == null)
		{
			throw new Error(n.loc + " you have to declare this struct first");
		}
		else
		{
			current_type = new Tstructp(structHT.get(n.id));
		}
	}     

	@Override
	public void visit(Pint n) {
	

			current_expr = new Econst(n.n);
			if (n.n == 0)
			{
				current_expr.typ = new Ttypenull();
			}
			else
			{
			current_expr.typ = new Tint();
			}
	}

	@Override
	public void visit(Pident n) {
		current_id = n.id;
		//System.out.println(current_id);
		boolean flag = false;
		for(int i = bloc_variableHT.size() -1; i > -1; i--)
		{
			if(bloc_variableHT.get(i).containsKey(n.id))
			{
				current_expr = new Eaccess_local(current_id);
				current_expr.typ = bloc_variableHT.get(i).get(n.id);
				flag = true;
				break;
			}
		}
		if(!flag)
		{
			throw new Error(n.loc + " the variable " + n.id + " has not been declared yet");
		}
		
	}

	@Override
	public void visit(Punop n) {
		// TODO Auto-generated method stub
		n.e1.accept(this);
		switch(n.op)
		{
			case Uneg:
				if(!(current_expr.typ instanceof Ttypenull || current_expr.typ instanceof Tint))
				{
					throw new Error(n.loc + " Negation works only for integers");
				}
				break;
			default:
				break;
		}		
		
		current_expr = new Eunop(n.op,current_expr);
		current_expr.typ = new Tint();
		
	}

	@Override
	public void visit(Passign n) {
		n.e1.accept(this); // partie gauche, forcement un identifiant en tout cas current id est tjr ms a jour
		Expr e1 = current_expr;
		Field e1_field = field_to_update;
		String id_to_assign = current_id;
		n.e2.accept(this); // partie droite, doit etre du type de n.e1
		
		//System.out.println(current_expr.typ);
		if(!(e1.typ instanceof Ttypenull || current_expr.typ instanceof Ttypenull || e1.typ.getClass() == current_expr.typ.getClass() || (e1.typ instanceof Tstructp && current_expr.typ instanceof Tvoidstar)))
		{
			throw new Error(n.loc + " Assignement should be done with variables of the same types");
		}
		if(e1 instanceof Eaccess_local)
		{
			current_expr = new Eassign_local(id_to_assign,current_expr);
			
			
		}
		else
		{
			Eaccess_field e1_b = (Eaccess_field) e1;
			current_expr = new Eassign_field(e1_b.e,e1_field,current_expr);
		}
		current_expr.typ = e1.typ;
		
	}

	@Override
	public void visit(Pbinop n) {
		n.e1.accept(this);
		Expr e1 = current_expr;
		n.e2.accept(this);
		switch(n.op) {
		case Beq:
		case Bneq:
		case Blt:
		case Ble:
		case Bgt:
		case Bge:
			if(! (e1.typ instanceof Ttypenull || current_expr.typ instanceof Ttypenull || e1.typ.getClass() == current_expr.typ.getClass()))
			{
				throw new Error(n.loc + " Trying to compare expressions with different types");
			}
			break;
		case Badd:
		case Bsub:
		case Bmul:
		case Bdiv:
			
			if(!((e1.typ instanceof Ttypenull || e1.typ instanceof Tint) && (current_expr.typ instanceof Tint || current_expr.typ instanceof Ttypenull)))
			{
				throw new Error(n.loc + " Unvalid arithmetical operations : expressions have to be integers");
			}
			break;
		default:
			break;
		}
		
		current_expr  = new Ebinop(n.op,e1,current_expr);
		current_expr.typ = new Tint();
		
	}

	@Override
	public void visit(Parrow n) {
		//System.out.println(n.f);
		n.e.accept(this);
		String id_to_evaluate = current_id;
		//System.out.println(id_to_evaluate);
		
			if( current_expr.typ instanceof Tstructp)
			{
				Tstructp current_struct = (Tstructp) current_expr.typ;
				if(structHT.get(current_struct.s.str_name).fields.get(n.f) ==null)
				{
					throw new Error(n.loc + " this structure does not have declared the field : " + n.f);
				}
				else
				{
					field_to_update = structHT.get(current_struct.s.str_name).fields.get(n.f);
					current_expr = new Eaccess_field(current_expr,field_to_update);
					current_expr.typ = field_to_update.field_typ;
					
				}
			}
		
		else
		{
			throw new Error(n.loc + " you have not declared yet this structure so you cannot assign its fields");
		
		}
		
		
		
	}

	@Override
	public void visit(Pcall n) {
		// TODO Auto-generated method stub
		//System.out.println(n.f);
		if(functionsHT.get(n.f) == null)
		{
			throw new Error(n.loc + " this funtion has not been declared yet");
		}
		Decl_fun fun_to_check = functionsHT.get(n.f);
		LinkedList<Expr> el = new LinkedList<Expr>();
		int i = 0;
		for(Pexpr expr : n.l)
		{
			expr.accept(this);
			if(!(current_expr.typ instanceof Ttypenull || current_expr.typ.getClass() == fun_to_check.fun_formals.get(i).t.getClass()))
			{
				throw new Error(n.loc + " Wrong type for one of the args");
			}
			el.add(current_expr);
			i++;
		}
		if(n.l.size() != functionsHT.get(n.f).fun_formals.size())
		{
			throw new Error(n.loc + " not enough / too many arugments in the call of function " + n.f);
		}
		current_expr = new Ecall(n.f,el);
		current_expr.typ = functionsHT.get(n.f).fun_typ;
	}

	@Override
	public void visit(Psizeof n) {
		if(!structHT.containsKey(n.id))
		{
			throw new Error(n.loc + " : The structure " + n.id + " has not been declared yet");
		}
		current_type = new Tint();
		current_expr = new Esizeof(structHT.get(n.id));
		current_expr.typ = new Tint();
		
	}

	@Override
	public void visit(Pskip n) {
		current_stmt = new Sskip();
	}

	@Override
	public void visit(Peval n) {
		// TODO Auto-generated method stub
		n.e.accept(this);
		current_stmt = new Sexpr(current_expr);
		
	}

	@Override
	public void visit(Pif n) {
		n.e.accept(this);
		Expr e = current_expr;
		n.s1.accept(this);
		Stmt s1 = current_stmt;
		n.s2.accept(this);
		current_stmt = new Sif(e,s1,current_stmt);
		
	}

	@Override
	public void visit(Pwhile n) {
		n.e.accept(this);
		Expr e = current_expr;
		
		n.s1.accept(this);
		
		current_stmt = new Swhile(e,current_stmt);
		
	}

	@Override
	public void visit(Pbloc n) {
		// TODO Auto-generated method stub
		//System.out.println(n.getClass());
		LinkedList<Decl_var> decl_var = new LinkedList<Decl_var>();
		bloc_variableHT.add(new HashMap<>());
		for(Pdeclvar n_var : n.vl)
		{
			n_var.typ.accept(this);
			
			
			if(bloc_variableHT.getLast().containsKey(n_var.id))
			{
				throw new Error(n_var.loc + "this variable is already declared");
			}
			
			bloc_variableHT.getLast().put(n_var.id, current_type);
			decl_var.add(new Decl_var(current_type,n_var.id));
		}
		LinkedList<Stmt> sl = new LinkedList<Stmt>();
		for(Pstmt s : n.sl )
		{
			s.accept(this);
			sl.add(current_stmt);
		}
		//System.out.println(bloc_variableHT.size());
		bloc_variableHT.pollLast();
		
		bloc = new Sblock(decl_var,sl);
		current_stmt = bloc;
	}

	@Override
	public void visit(Preturn n) {
		// TODO Auto-generated method stub
		n.e.accept(this);
		if(! (current_expr.typ.getClass() == fun_type.getClass() || current_expr.typ instanceof Ttypenull))
			{
				throw new Error(n.loc + " You cannot return a different type from the declared one above");
			}
		current_stmt = new Sreturn(current_expr);
	}

	@Override
	public void visit(Pstruct n) {
		int current_position = 0;
		if(!(structHT.get(n.s) == null))
		{
			throw new Error("A struct named " + n.s + "has already been declared"); 
		}
		structHT.put(n.s, new Structure(n.s));
		Field current_field;
		for(Pdeclvar n_var : n.fl)
		{
			n_var.typ.accept(this);
			current_field = new Field(n_var.id,current_type,current_position);
			
			current_position += 8;
			if(!(structHT.get(n.s).fields.get(n_var.id) == null))
			{
				throw new Error(n_var.loc + " You cannot declare two fields with the same name");
			}
			structHT.get(n.s).fields.put(n_var.id,current_field);
			
		}
		structHT.get(n.s).struct_size = current_position;
		
	}

	@Override
	public void visit(Pfun n) {
		
		if(functionsHT.containsKey(n.s))
		{
			throw new Error(n.loc + " A function named : " + n.s + "has already been declared");
		}
		n.ty.accept(this);
		fun_type = current_type;
		LinkedList<Decl_var> decl_var = new LinkedList<Decl_var>();
		bloc_variableHT.add(new HashMap<>());
		
		for(Pdeclvar n_var : n.pl)
		{
			n_var.typ.accept(this);
			for(int i = bloc_variableHT.size() -1; i > -1; i--)
			{
				if(bloc_variableHT.get(i).containsKey(n_var.id))
				{
					throw new Error(n_var.loc + "this variable is already declared");
				}
			}
			bloc_variableHT.getLast().put(n_var.id, current_type);
			decl_var.add(new Decl_var(current_type,n_var.id));
		}
		functionsHT.put(n.s,new Decl_fun(fun_type,n.s,decl_var,null));
		n.b.accept(this);
		functionsHT.put(n.s,new Decl_fun(fun_type,n.s,decl_var,bloc));
		funs.add(new Decl_fun(fun_type,n.s,decl_var,bloc));
		bloc_variableHT.pollLast();
	}

}
