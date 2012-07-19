package info.radm.scanner.hmmer;



import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Class HmmerParser
 * 
 * Parses the output obtained from running hmmscan to xdom.
 * 
 * @author Andrew D. Moore <radmoore@uni-muenster.de>
 */
public class HmmerParser {
	
	private File domtblout, outfile;
	private boolean merge = false, resolveOverlaps = false, collapse = false, accMode = false,
			removeEmpties = false, clanMode = false;
	private Double evalue = null;
	private int repNo = 0;
	public static int HMMSCAN = 0;
	public static int PFAMSCAN = 1;
	public static int UNKNOWN = -1;
	
	
	public HmmerParser(String domtbloutPath, String outfilePath) {
		
		try {
			domtblout = new File(domtbloutPath);
			outfile = new File(outfilePath);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static int determineFileFormat(String domtbloutPath) {
		
		String line = null;
		int type = -1;
		try {
			FileInputStream fis = new FileInputStream(new File(domtbloutPath));
			DataInputStream dis = new DataInputStream(fis);
			BufferedReader br = new BufferedReader(new InputStreamReader(dis));
			Pattern comment = Pattern.compile("^#.*");
			Pattern empty = Pattern.compile("^$");
			Pattern pfamId = Pattern.compile("PF\\d+.\\d+");
			while ( (line = br.readLine()) != null ) {
				
				if (comment.matcher(line).matches())
					continue;
				if (empty.matcher(line).matches())
					continue;
				
				
				String[] fields = line.split("\\s+");
				if (fields.length <= 18) {
					
					Matcher m = pfamId.matcher(fields[5]);
					if (m.find()) {
						type = PFAMSCAN;
						break;
					}
				}
				else if (fields.length > 18) {
					Matcher m = pfamId.matcher(fields[1]);
					if (m.find()) {
						type = HMMSCAN;
						break;
					}
				}
			}
			br.close();
			dis.close();
			fis.close();
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return type;
	}
	
	public void setMergeMode() {
		this.merge = true;
	}
	
	public void setCollapseMode(int repNo) {
		this.collapse = true;
		this.repNo = repNo;
	}
	
	public void setAccMode() {
		this.accMode = true;
	}
	
	public void setClanMode() {
		this.clanMode = true;
	}
	
	public void setResolveOverlapsMode() {
		this.resolveOverlaps = true;
	}
	
	public void setRemoveEmpties() {
		this.removeEmpties = true;
	}
	
	// handle wrong format _before_ running hmmer
	public void setEvalueThreshold(Double evalue) {
		this.evalue = evalue;
	}
	
	public File getHmmerOutput(){
		return this.domtblout;
	}
	
	public void destroyHmmoutFile () {
		this.domtblout.delete();
	}
	
	public boolean isHmmscanOut() {
		return true;
	}
	
	public boolean isPfamscanOut() {
		return true;
	}
	
	public void hmmscan2xdom() {
		
		TreeMap<Integer, Domain> currentDoms = new TreeMap<Integer, Domain>();
		Pattern comment = Pattern.compile("^#.+");
		
		try {
			FileWriter fw = new FileWriter(outfile);
					
			String line;
			FileInputStream fis = new FileInputStream(this.domtblout);
			DataInputStream dis = new DataInputStream(fis);
			BufferedReader br = new BufferedReader(new InputStreamReader(dis));
			String currentId = null;
			StringBuilder xdom = new StringBuilder();
			int didField = 0;
			
			if ( accMode )
				didField = 1 ;
			
			
			while((line = br.readLine())!= null) {	
				if (comment.matcher(line).matches())
					continue;
				
				String[] fields = line.split("\\s+");
				
				// 0 -> domain id
				// 1 -> domain acc
				// 3 -> protein ID
				// 5 -> protein length
				// 12 -> domain I-evalue
				// 15, 16 -> hmm coord
				// 17, 18 -> align coord
				// 19, 20 -> env coord
				String thisId = fields[3];
				Pattern p = Pattern.compile("\\w+.\\d+");
				Matcher m = p.matcher(thisId);
				if (m.find()) {
					String[] pidFields = thisId.split("\\.");
					thisId = pidFields[0];
				}
				if ( (currentId != null) && (!thisId.equals(currentId)) ) {
					if (xdom.length() != 0) {

						if (! (currentDoms.isEmpty() && removeEmpties))
							fw.write(xdom.toString()+"\n");

						if (! currentDoms.isEmpty() ) {
							// merge split hits
							if ( merge )
								currentDoms = mergeHits( currentDoms );
			
							// resolve overlaps
							if ( resolveOverlaps )
								resolveOverlaps( currentDoms, null );
													
							// write the rest of the domains
							for (int key : currentDoms.keySet()) {
								Domain cdom;
								if ( (cdom = currentDoms.get(key)) != null)
										fw.write(cdom.toString()+"\n");
							}
						}
					}
					xdom.setLength(0);
					currentDoms.clear();
				}
				
				if (xdom.length() == 0) {
					currentId = fields[3];
					p = Pattern.compile("\\w+.\\d+");
					m = p.matcher(currentId);
					if (m.find()) {
						String[] pidFields = currentId.split("\\.");
						currentId = pidFields[0];
					}
					System.out.println("Current id: "+currentId);
					xdom.setLength(0);
					xdom.append(">"+currentId+"\t"+fields[5]);
				}
				if (evalue != null)
					if (Double.parseDouble(fields[12]) > evalue)
						continue;
			
				// ensure that the version number is removed if we are
				// in acc mode
				String did = fields[didField] ;
				if ( accMode ) {
					p = Pattern.compile("PF\\d+.\\d+");
					m = p.matcher(did);
					if (m.find()) {
						String[] didFields = did.split("\\.");
						did = didFields[0];
					}
				}
				
				Domain dom = new Domain(did,
						Integer.parseInt(fields[17]), 
						Integer.parseInt(fields[18]),
						Integer.parseInt(fields[15]),
						Integer.parseInt(fields[16]),
						Double.parseDouble(fields[12]));
				
				currentDoms.put(Integer.parseInt(fields[17]), dom);
			}
			if (xdom.length() != 0) {
				if (! (currentDoms.isEmpty() && removeEmpties))
					fw.write(xdom.toString()+"\n");
				if (! currentDoms.isEmpty() ) {
					if ( merge )
						currentDoms = mergeHits( currentDoms );
					
					if ( resolveOverlaps )
						resolveOverlaps( currentDoms, null );
				
					for (int key : currentDoms.keySet()) {
						Domain cdom = currentDoms.get(key);
						if ( (cdom = currentDoms.get(key)) != null)
							fw.write(cdom.toString()+"\n");
					}
				}
			}			
			fis.close();
			dis.close();
			br.close();
			fw.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}	
	}
	
	//<seq id> <alignment start> <alignment end> <envelope start> <envelope end> 
	//<hmm acc> <hmm name> <type> <hmm start> <hmm end> <hmm length> <bit score> <E-value> <significance> <clan>
	public void pfamscan2xdom() {
		
		TreeMap<Integer, Domain> currentDoms = new TreeMap<Integer, Domain>();
		Pattern comment = Pattern.compile("^#.*");
		Pattern empty = Pattern.compile("^$");
		
		try {
			FileWriter fw = new FileWriter(outfile);
					
			String line;
			FileInputStream fis = new FileInputStream(this.domtblout);
			DataInputStream dis = new DataInputStream(fis);
			BufferedReader br = new BufferedReader(new InputStreamReader(dis));
			String currentId = null;
			StringBuilder xdom = new StringBuilder();
			int didField = 6;
			
			if ( accMode )
				didField = 5;
			
			while((line = br.readLine())!= null) {	
				if (comment.matcher(line).matches())
					continue;
				if (empty.matcher(line).matches())
					continue;
				
				String[] fields = line.split("\\s+");
				
				// 0 -> protein id
				// 5 -> domain acc
				// 6 -> domain name (id)
				// 12 -> domain evalue
				// 8, 9 -> hmm coord
				// 1, 2 -> align coord
				// 3, 4 -> env coord
				// 15 -> clanid 
				String thisId = fields[0];
				Pattern p = Pattern.compile("\\w+.\\d+");
				Matcher m = p.matcher(thisId);
				if (m.find()) {
					String[] pidFields = thisId.split("\\.");
					thisId = pidFields[0];
				}
				if ( (currentId != null) && (!thisId.equals(currentId)) ) {
					if (xdom.length() != 0) {
						fw.write(xdom.toString()+"\n");
						// merge split hits
						if ( merge )
							currentDoms = mergeHits( currentDoms );
		
						// resolve overlaps
						if ( resolveOverlaps )
							resolveOverlaps( currentDoms, null );
		
						if ( collapse )
							currentDoms = collapseRepeats( currentDoms );
						
						// write the rest of the domains
						for (int key : currentDoms.keySet()) {
							Domain cdom = currentDoms.get(key);
							if ( (cdom = currentDoms.get(key)) != null)
								fw.write(cdom.toString()+"\n");
						}
					}
					xdom.setLength(0);
					currentDoms.clear();
				}
				
				if (xdom.length() == 0) {
					currentId = fields[0];
					p = Pattern.compile("\\w+.\\d+");
					m = p.matcher(currentId);
					if (m.find()) {
						String[] pidFields = currentId.split("\\.");
						currentId = pidFields[0];
					}
					xdom.setLength(0);
					xdom.append(">"+currentId);
				}
				if (evalue != null)
					if (Double.parseDouble(fields[12]) > evalue)
						continue;
			
				// ensure that the version number is removed if we are
				// in acc mode
				String did = fields[didField] ;
				if ( accMode ) {
					p = Pattern.compile("PF\\d+.\\d+");
					m = p.matcher(did);
					if (m.find()) {
						String[] didFields = did.split("\\.");
						did = didFields[0];
					}
				}
				if ( clanMode ) {
					p = Pattern.compile("CL\\d+");
					m = p.matcher(fields[14]);
					if (m.find())
						did = fields[14];
				}

				Domain dom = new Domain(did,
						Integer.parseInt(fields[1]), 
						Integer.parseInt(fields[2]),
						Integer.parseInt(fields[8]),
						Integer.parseInt(fields[9]),
						Double.parseDouble(fields[12]));
				
				currentDoms.put(Integer.parseInt(fields[1]), dom);
			}
			if (xdom.length() != 0) {
				fw.write(xdom.toString()+"\n");
				if ( merge )
					currentDoms = mergeHits( currentDoms );
				
				if ( resolveOverlaps )
					resolveOverlaps( currentDoms, null );
			
				if ( collapse )
					currentDoms = collapseRepeats( currentDoms );
				
				for (int key : currentDoms.keySet()) {
					Domain cdom = currentDoms.get(key);
					if ( (cdom = currentDoms.get(key)) != null)
						fw.write(cdom.toString()+"\n");
				}
			}			
			fis.close();
			dis.close();
			br.close();
			fw.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}	
	}
	
	private TreeMap<Integer, Domain> collapseRepeats(TreeMap<Integer, Domain> doms) {
		
		TreeMap<Integer, Domain> collpasedDomains = new TreeMap<Integer, Domain>();
		ArrayList<Domain> domainHolding = new ArrayList<Domain>();
		
		Domain lastDom = null;
		for (Domain cDom: doms.values()) {
			
			if (lastDom == null) {
				domainHolding.add(cDom);
				lastDom = cDom;
				continue;
			}
			
			if ( lastDom.ID.equals(cDom.ID) ) 
				domainHolding.add(cDom);
			
			else {
				if (domainHolding.size() >= repNo) {
					Domain firstRepDom = domainHolding.get(0);
					Domain d = new Domain(lastDom.ID, firstRepDom.aliFrom, lastDom.aliTo, -1, -1, -1);
					d.setComment("collapsed "+domainHolding.size()+" instances");
					collpasedDomains.put(d.aliFrom, d);
				}
				else {
					for (Domain d : domainHolding)
						collpasedDomains.put(d.aliFrom, d);
					
				}
				domainHolding.clear();
				domainHolding.add(cDom);
			}
			lastDom = cDom;
		}
		if (domainHolding.size() >= repNo) {
			Domain firstRepDom = domainHolding.get(0);
			Domain d = new Domain(lastDom.ID, firstRepDom.aliFrom, lastDom.aliTo, -1, -1, -1);
			d.setComment("collapsed "+domainHolding.size()+" instances");
			collpasedDomains.put(d.aliFrom, d);
		}
		else {
			for (Domain d : domainHolding)
				collpasedDomains.put(d.aliFrom, d);
		}
		return collpasedDomains;
	}
	
	
	private TreeMap<Integer, Domain> mergeHits(TreeMap<Integer, Domain> doms) {
		
		Domain lastDom = null, mergedDom = null;
		int lastStart = 0;
		int numOfMerged = 1; // n merge operations means n+1 merged domains
		TreeMap<Integer, Domain> modifedDoms = new TreeMap<Integer, Domain>();
		for ( int startPos : doms.keySet() ) {
			
			Domain curDom = doms.get(startPos);
			
			if (lastDom != null) {
				// if the same domain type...
				if (lastDom.getID().equals(curDom.getID())) {
					// ... check if split hits present
					if ( (lastDom.getHmmTo() < curDom.getHmmFrom()) &&
							(lastDom.getAliTo() < curDom.getAliFrom()) ) {
						
							numOfMerged += 1;
						// if we alread started a merged domain, extend
						if (mergedDom != null){
							mergedDom.setAliTo(curDom.getAliTo());
							mergedDom.setComment(numOfMerged+" merged hits");
						}
						// else start new merged Domain
						else {
							mergedDom = new Domain(curDom.getID(), 
									lastDom.getAliFrom(), // from beginning of last domain
									curDom.getAliTo(),    // to end of current domain
									lastDom.getHmmFrom(),
									curDom.getHmmTo(),
									-1);  // evalue?
							mergedDom.setComment(numOfMerged+" merged hits");
						}
					}
					// last domain ID is the same, but hits are not split
					else {
						modifedDoms.put(lastStart, lastDom);
					}
				}
				// last domain ID is different from current domain ID
				else {
					//first add merged domain, if present
					if (mergedDom != null) {
						modifedDoms.put(mergedDom.aliFrom, mergedDom);
					}
					// all is well - we will keep the last domain
					else { 
						modifedDoms.put(lastStart, lastDom);	
					}
					mergedDom = null;
					numOfMerged = 1;
				}
			}
			lastStart = startPos;
			lastDom = curDom;
		}
		if (mergedDom != null) {
			mergedDom.setComment(numOfMerged+" merged hits");
			modifedDoms.put(mergedDom.aliFrom, mergedDom);
		}
		// all is well - we will keep the last domain
		else
			modifedDoms.put(lastStart, lastDom);
		
		return modifedDoms;
	}
	
	private void resolveOverlaps(TreeMap<Integer, Domain> doms, Domain lastDom) {
		
		ArrayList<Integer> flaggedToRemove = new ArrayList<Integer>();
		int lastStart = 0;
		for ( int startPos : doms.keySet() ) {
			
			Domain curDom = doms.get(startPos);
			if (lastDom != null) {
				if (lastDom.overlaps(curDom)) {
					if (lastDom.getEvalue() >= curDom.getEvalue())
						flaggedToRemove.add(lastStart);
						else
							flaggedToRemove.add(startPos);
				}
			}
			lastStart = startPos;
			lastDom = curDom;
		}
		if (! flaggedToRemove.isEmpty() ) {
			for (int remPos : flaggedToRemove)
				doms.remove(remPos);
			
			resolveOverlaps(doms, null);
		}
		
	}
	
	
	private class Domain{
		
		private int aliFrom, aliTo, hmmFrom, hmmTo;
		private double evalue;
		private String ID, comment;
		private boolean isCollapsed = false;
		
		public Domain(String ID, int aliFrom, int aliTo, int hmmFrom, int hmmTo, double evalue) {
			this.aliFrom = aliFrom;
			this.aliTo = aliTo;
			this.hmmFrom = hmmFrom;
			this.hmmTo = hmmTo;
			this.evalue = evalue;
			this.ID = ID;
		}
		
		public void setAliTo(int endPos) {
			this.aliTo = endPos;
		}
		
		public void setComment(String comment) {
			this.comment = comment;
		}
		
		public void setCollpased() {
			this.isCollapsed = true;
		}
		
		public int getAliFrom() {
			return this.aliFrom;
		}
		
		public int getAliTo() {
			return this.aliTo;
		}
		
		public int getHmmFrom() {
			return this.hmmFrom;
		}
		
		public int getHmmTo() {
			return this.hmmTo;
		}
		
		public String getID() {
			return this.ID;
		}
		
		public double getEvalue() {
			return this.evalue;
		}

		public boolean isCollapsed() {
			return this.isCollapsed;
		}
		
		public String toString() {
			if (comment == null)
				return this.aliFrom+"\t"+this.aliTo+"\t"+this.ID+"\t"+this.evalue;
			else
				return this.aliFrom+"\t"+this.aliTo+"\t"+this.ID+"\t"+this.evalue+"\t;"+this.comment;
		}
		
		public boolean overlaps(Domain nextDom) {
			if (this.aliTo >= nextDom.aliFrom)
				return true;
			return false;
		}
		
	}
	
}
