package eddy.lang.analysis;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import eddy.lang.Action;
import eddy.lang.parser.Compilation;
import eddy.lang.parser.Logger;
import eddy.lang.parser.ParseException;

/**
 * Extends the {@link ConflictAnalyzer} to multi-threaded analysis. The {@link eddy.lang.Policy} 
 * is segmented and separately compiled in a cache, where each policy segment is analyzed
 * and the results are recombined to detect the complete set of {@link Conflict}s.
 * 
 * @author Travis Breaux
 *
 */

public class ExtendedConflictAnalyzer {
	private Logger logger = new Logger(new PrintWriter(System.err), Logger.WARN, this.getClass().getName() + ": ");
	private int blockSize = 1000;
	private int threadCount = 3;
	private TreeSet<Conflict> conflicts;
	private String cachePath = null;
	
	public ArrayList<Conflict> analyze(Compilation comp) {
		this.conflicts = new TreeSet<Conflict>();
		
		// compute the extensions for this ontology
		ExtensionCalculator calculator = new ExtensionCalculator();
		ArrayList<Action> actions;
		try {
			actions = calculator.compute(comp);
			
		} catch (ParseException e) {
			actions = new ArrayList<Action>();
			e.printStackTrace();
		}
		logger.log(Logger.DEBUG, "Calculated " + actions.size() + " actions in the extensions");
		
		// separate the actions into work blocks
		ArrayList<List<Action>> blocks = new ArrayList<List<Action>>();
		for (int i = 0; i < actions.size(); i += blockSize) {
			blocks.add(actions.subList(i, Math.min(actions.size(), i + blockSize)));
		}
		
		// distribute blocks to workers
		distribute(blocks, comp);
		logger.log(Logger.DEBUG, "Detected " + conflicts.size() + " unique conflicts");
		
		return new ArrayList<Conflict>(conflicts);
	}
	
	private void distribute(ArrayList<List<Action>> blocks, Compilation comp) {
		Worker[] worker = new Worker[threadCount];
		Thread[] thread = new Thread[threadCount];
		
		// initialize workers with shared list
		for (int i = 0; i < worker.length; i++) {
			worker[i] = new Worker();
		}
		
		logger.log(Logger.DEBUG, "Dispatching " + blocks.size() + " work blocks to " + threadCount + " workers...");
		for (int i = 0; i < thread.length; i++) {
			thread[i] = new Thread();
		}

		// begin distribution of blocks to workers
		int index = 0;
		boolean running = true;
		while (running) {
			running = false;
			for (int i = 0; i < thread.length; i++) {
				if (thread[i] == null) {
					continue;
				}
				else if (thread[i].isAlive()) {
					running = true;
				}
				else {
					// check if this worker has any uncollected work
					if (worker[i].conflicts != null) {
						logger.log(Logger.DEBUG, "Received block " + worker[i].index + " with " + worker[i].conflicts.size() + " conflicts");
						this.conflicts.addAll(worker[i].conflicts);
						worker[i].conflicts = null;
					}
					
					// start a new thread for any remaining blocks
					if (index < blocks.size()) {
						List<Action> block = blocks.get(index);
						int counter = index * blockSize;
						worker[i].index = index;
						worker[i].extComp = ExtensionCalculator.extend(comp, block, counter);
						index++;
						
						if (cachePath != null) {
							File file = new File(cachePath + "/part" + index + ".owl");
							worker[i].extComp.save(file);
						}
						
						thread[i] = new Thread(worker[i]);
						thread[i].start();
						running = true;
					}
					else {
						thread[i] = null;
					}
				}
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void setCachePath(String path) {
		this.cachePath = path;
	}
	
	class Worker implements Runnable {
		private Extension extComp;
		private ConflictAnalyzer analyzer = new ConflictAnalyzer();
		private ArrayList<Conflict> conflicts = null;
		private int index;
		
		public Worker() {
			return;
		}
		public void run() {
			try {
				this.conflicts = analyzer.analyze(extComp);
			}
			catch (Exception e) {
				conflicts = new ArrayList<Conflict>();
				e.printStackTrace();
			}
		}
	}
}
