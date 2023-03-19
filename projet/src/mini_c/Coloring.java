package mini_c;

import java.awt.Color;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Coloring {
	Map<Register, Operand> colors = new HashMap<>();
	 int nlocals = 0; // nombre d'emplacements sur la pile
	 
	 Coloring(Interference inter)
	 {
		 Set<Register> todo = new HashSet<>();
		 for(Map.Entry<Register, Arcs> entry : inter.graph.entrySet())
		 {
			 if(entry.getKey().isPseudo())
			 {
				 todo.add(entry.getKey());
			 }
			 else
			 {
				 colors.put(entry.getKey(),new Reg(entry.getKey()));
			 }
		 }
		 Map <Register,Set<Register>> possible_colors = new HashMap<>();
		 for(Register r : todo)
		 {
			 // On initialise les couleurs possibles pour le coloriage
			 Set<Register> color = new HashSet<>();
			 color.addAll(Register.allocatable);
			 color.removeAll(inter.graph.get(r).intfs);
			 possible_colors.put(r, color);
		 }
		 
		 while(!todo.isEmpty())
		 {
			 boolean flag = false;
			 for(Register entry : todo)
			 {
				 if(possible_colors.get(entry).size() == 1 && inter.graph.get(entry).prefs.contains(possible_colors.get(entry).toArray()[0]))
				 {
					 Register col = (Register) possible_colors.get(entry).toArray()[0];
					 remove_color(inter,possible_colors,entry,col);
					 todo.remove(entry);
					 flag = true;
					 
				 }
			 }
			 if(!flag)
			 {
				 for(Register entry : todo)
				 {
					 if(possible_colors.get(entry).size() == 1)
					 {
						 Register col = (Register) possible_colors.get(entry).toArray()[0];
						 remove_color(inter,possible_colors,entry,col);
						 todo.remove(entry);
						 flag = true;
						 break;
					 }
				 }
			 }
			 if(!flag)
			 {
				
				 for(Register entry : todo)
				 {
					 
					 for(Register pref : inter.graph.get(entry).prefs)
					 {
						
						 if(colors.containsKey(pref) && colors.get(pref) instanceof Reg && !flag)
						 {
							 
							 Reg actual_color = (Reg) colors.get(pref);
							 remove_color(inter,possible_colors,entry,actual_color.r);
							 todo.remove(entry);
							 flag = true;
						 }
					 }
					 if(flag)
					 {
						 break;
					 }
				 } 
			 }
			 if(!flag)
			 {
				 for(Register entry : todo)
				 {
					 if(possible_colors.get(entry).size()> 0 )
					 {
						 Register col = (Register) possible_colors.get(entry).toArray()[0];
						 remove_color(inter,possible_colors,entry,col);
						 todo.remove(entry);
						 flag = true;
						 break;
					 }
				 }
			 }
			 if(!flag)
			 {
				 Register reg = (Register) todo.toArray()[0];
				 nlocals++;
				 Spilled new_spill = new Spilled(nlocals*8);
				 todo.remove(reg);
				 colors.put(reg, new_spill);
			 }
		 }
	 
	 }
	 void remove_color(Interference inter,Map<Register,Set<Register>> possible_colors,Register pseudo,Register color)
	 {
		 Reg col_to_remove = new Reg(color);
		 colors.put(pseudo, col_to_remove);
		 for(Register interfe : inter.graph.get(pseudo).intfs)
		 {
			 if(interfe.isPseudo() && possible_colors.get(interfe).size() != 0)
			 {
			 possible_colors.get(interfe).remove(color);
			 }
		 }
	 }
	 void print() {
		    System.out.println("coloring output:");
		    for (Register r: colors.keySet()) {
		      Operand o = colors.get(r);
		      System.out.println("  " + r + " --> " + o);
		    }
		  }
}
