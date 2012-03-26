package parser;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.TreeMap;
import java.util.regex.Pattern;

public class HmmerParser {
	
	private File domtblout, outfile;
	private Boolean merge = false, resolveOverlaps = false, collapse = false;
	private Double evalue = null;
	
	public HmmerParser(String domtbloutPath, String outfilePath) {
		try {
			domtblout = new File(domtbloutPath);
			outfile = new File(outfilePath);
		}
		catch (Exception e) {
			// do something
		}
	}
	
	public void setMergeMode() {
		this.merge = true;
	}
	
	public void setCollapseMode() {
		this.collapse = true;
	}
	
	public void setResolveOverlapsMode() {
		this.resolveOverlaps = true;
	}
	
	// handle wrong format _before_ running hmmer
	public void setEvalueThreshold(Double evalue) {
		this.evalue = evalue;
	}
	
	public File getHmmerOutput(){
		return this.domtblout;
	}
	
	public void destoryTempFile () {
		this.domtblout.delete();
	}
	
	public void writeXdom() {
		
		TreeMap<Integer, String> currentDoms = new TreeMap<Integer, String>();
		Pattern comment = Pattern.compile("^#.+");
		
		try {
			FileWriter fw = new FileWriter(outfile);
					
			String line;
			FileInputStream fis = new FileInputStream(this.domtblout);
			DataInputStream dis = new DataInputStream(fis);
			BufferedReader br = new BufferedReader(new InputStreamReader(dis));
			String currentId = null;
			StringBuilder xdom = new StringBuilder();
			
			
			while((line = br.readLine())!= null) {
				
				if (comment.matcher(line).matches())
					continue;
				
				String[] fields = line.split("\\s+");
				
				// 0 -> domain acc
				// 3 -> protein ID
				// 5 -> protein length
				// 12 -> domain I-evalue
				// 15, 16 -> hmm coord
				// 17, 18 -> align coord
				// 19, 20 -> env coord
				//System.out.println(fields[5]);
				
				if ( (currentId != null) && (!fields[3].equals(currentId)) ) {
					
					if (xdom.length() != 0) {
						fw.write(xdom.toString()+"\n");
						for (int key : currentDoms.keySet())
							fw.write(currentDoms.get(key)+"\n");							
					}
					xdom.setLength(0);
					currentDoms.clear();
				}
				
				if (xdom.length() == 0) {
					currentId = fields[3];
					xdom.setLength(0);
					xdom.append(">"+currentId+"\t"+fields[5]);
				}
				if (evalue != null)
					if (Double.parseDouble(fields[12]) > evalue)
						continue;
				
				currentDoms.put(Integer.parseInt(fields[17]), 
						fields[17]+"\t"+fields[18]+"\t"+fields[0]+"\t"+fields[12]);
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
	
}
