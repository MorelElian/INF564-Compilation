package mini_c;

import java.security.KeyStore.Entry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
class LiveInfo {
    ERTL instr;
	 Label[] succ;   // successeurs
	Set<Label> pred;   // prédécesseurs
	Set<Register> defs;   // définitions
	Set<Register> uses;   // utilisations
	Set<Register> ins;    // variables vivantes en entrée
	Set<Register> outs;   // variables vivantes en sortie
	LiveInfo(ERTL instr,Set<Register> defs, Set<Register> uses, Label[] succ)
	{
		this.instr = instr;
		this.defs = defs;
		this.uses = uses;
		this.succ = succ;
		this.pred = new HashSet<>();
		this.ins = new HashSet<>();
		this.outs = new HashSet<>();
		
	}
	public String toString()
	{
		return(this.instr +  " defs : " + this.defs + " uses : "+ this.uses + " ins :" + this.ins + " out : " + this.outs );
	}
	}
public class Liveness {
	 Map<Label, LiveInfo> info;
	 
	  Liveness(ERTLgraph g) {
		  info = new HashMap<>();
		  for(Map.Entry<Label, ERTL> entry : g.graph.entrySet()) {
			  ERTL current_ERTL = entry.getValue();
			  //System.out.println(current_ERTL);
			  //System.out.println(current_ERTL.def());
			  info.put(entry.getKey(),new LiveInfo(current_ERTL,
					  current_ERTL.def(),
					  current_ERTL.use(),
					  current_ERTL.succ()));
			  
			  
		  }
		  for(Map.Entry<Label, LiveInfo> entry: info.entrySet())
		  {
			  for(Label succ : entry.getValue().succ)
			  {
				  info.get(succ).pred.add(entry.getKey());
			  }
		  }
		  // Algorithme de Kidall
		  Set<Map.Entry<Label,LiveInfo>> WS = info.entrySet();
		  while(!WS.isEmpty())
		  {
		
			  Set<Map.Entry<Label, LiveInfo>> nextWS = new HashSet<>();
			  for(Map.Entry<Label, LiveInfo> entry : WS)
			  {
				  Label current_label  = entry.getKey();
				  Set<Register> old_in = new HashSet<>();
				  old_in.addAll(info.get(current_label).ins);
				  //System.out.println(old_in);
				  for(Label succ : info.get(current_label).succ)
				  {
					  
		
					  info.get(current_label).outs.addAll(info.get(succ).ins); 
					  
				  }
				 
				  info.get(current_label).ins.removeAll(info.get(current_label).ins);
				  info.get(current_label).ins.addAll(info.get(current_label).uses);
				  Set<Register> tmp = new HashSet<>();
				  tmp.addAll(info.get(current_label).outs);
				  tmp.removeAll(info.get(current_label).defs);
				  info.get(current_label).ins.addAll(tmp);
			  
				  if(!info.get(current_label).ins.equals(old_in))
				  {
					  for(Label pred : info.get(current_label).pred)
					  {
						  
						  //System.out.println(entry.getKey());
						  nextWS.add(Map.entry(pred, info.get(pred)));
					  }
				  }
			  }
			  
			  WS = nextWS;
		  }
	  }
	  private void print(Set<Label> visited, Label l) {
		    if (visited.contains(l)) return;
		    visited.add(l);
		    LiveInfo li = this.info.get(l);
		    System.out.println("  " + String.format("%3s", l) + ": " + li);
		    for (Label s: li.succ) print(visited, s);
		  }

		  void print(Label entry) {
		    print(new HashSet<Label>(), entry);
		  }
}
