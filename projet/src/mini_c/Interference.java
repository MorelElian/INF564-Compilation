package mini_c;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Interference {

	Map<Register, Arcs> graph;
	
	Interference(Liveness lg) {
	graph = new HashMap<>();
	for(Map.Entry<Label, LiveInfo> entry : lg.info.entrySet())
	    {
			
	    	ERTL current_instr = lg.info.get(entry.getKey()).instr;
	    	if(current_instr instanceof ERmbinop)
	    	{
	    		
	    		ERmbinop current_instr_b = (ERmbinop) current_instr;
	    		
	    		if(current_instr_b.m == Mbinop.Mmov)
	    		{
	    			
	    			if(graph.get(current_instr_b.r1) == null)
	    			{
	    				
	    				Arcs arcs = new Arcs();
	    				arcs.prefs.add(current_instr_b.r2);
	    				graph.put(current_instr_b.r1, arcs);
	    				
	    				
	    			}
	    			else
	    			{
	    				graph.get(current_instr_b.r1).prefs.add(current_instr_b.r2);
	    			}
	    			if(graph.get(current_instr_b.r2) == null)
	    			{
	    				Arcs arcs = new Arcs();
	    				arcs.prefs.add(current_instr_b.r1);
	    				graph.put(current_instr_b.r2, arcs);
	    				
	    			}
	    			else
	    			{
	    				graph.get(current_instr_b.r2).prefs.add(current_instr_b.r1);
	    			}
	    		}
	    	}
	    }
	
	    // Construction des inférences
    for(Map.Entry<Label, LiveInfo> entry : lg.info.entrySet())
    {
    	ERTL current_instr = entry.getValue().instr;
    	
		for(Register def : entry.getValue().defs)
		{
			for(Register out : entry.getValue().outs)
			{
				if(!out.equals(def))
				{
    				if(graph.get(def) == null)
    				{
    					Arcs arc = new Arcs();
    					arc.intfs.add(out);
    					graph.put(def,arc);
    				}
    				else
    				{
    					graph.get(def).intfs.add(out);
    				}
    				if(graph.get(out) == null)
    				{
    					Arcs arc = new Arcs();
    					arc.intfs.add(def);
    					graph.put(out, arc);
    				}
    				else
    				{
    					graph.get(out).intfs.add(def);
    				}
				}
			}
		}
		if(current_instr instanceof ERmbinop)
		{
			ERmbinop current_instr_b = (ERmbinop) current_instr;
			if(graph.get(current_instr_b.r1) != null && graph.get(current_instr_b.r1).intfs.contains(current_instr_b.r2))
			{
				graph.get(current_instr_b.r1).prefs.remove(current_instr_b.r1);
				graph.get(current_instr_b.r2).prefs.remove(current_instr_b.r2);	
			}
		}
    	
	  }
	    
	}
	void print() {
	    System.out.println("interference:");
	    for (Register r: graph.keySet()) {
	      Arcs a = graph.get(r);
	      System.out.println("  " + r + " pref=" + a.prefs + " intf=" + a.intfs);
	    }
	  }
}
class Arcs {
	Set<Register> prefs = new HashSet<>();
	Set<Register>intfs = new HashSet<>();
		}
