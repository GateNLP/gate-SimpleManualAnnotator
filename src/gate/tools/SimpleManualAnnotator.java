package gate.tools;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.Utils;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.Border;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

public class SimpleManualAnnotator extends JPanel implements ActionListener {
   /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
    /**
     * Create the GUI and show it.  For thread safety, 
     * this method should be invoked from the 
     * event-dispatching thread.
     */

	public enum Mode {
		OPTIONSFROMTYPEANDFEATURE, OPTIONSFROMFEATURE, OPTIONSFROMSTRING
	}
		
	static String back = "Back";
	static String next = "Next";
	static String saveandexit = "Save and Exit";
	
	static File corpusdir = null;
	Document currentDoc;
	List<Annotation> mentionList;
	
	int currentDocIndex = -1;
	static int docsInDir;
	int currentAnnIndex = -1;
	int mentionsInDoc = -1;
	
	JButton backButton, nextButton, exitButton;
	JLabel progress;
	JEditorPane display = new JEditorPane();
    ButtonGroup optionGroup = new ButtonGroup();
	JPanel optionsFrame = new JPanel();
	
	AnnotationTask currentAnnotationTask;
	
	Configuration config;
	
    public SimpleManualAnnotator(File conf) {
		config = new Configuration(conf);
    	next();
    	
    	JPanel dispFrame = new JPanel();
    	dispFrame.setLayout(new BoxLayout(dispFrame, BoxLayout.Y_AXIS));
    	dispFrame.setAlignmentX(JComponent.CENTER_ALIGNMENT);
    	dispFrame.setBackground(Color.WHITE);
    	
    	progress = new JLabel();
    	progress.setPreferredSize(new Dimension(250, 40));
    	progress.setMinimumSize(new Dimension(10, 10));
    	dispFrame.add(progress);

    	display.setEditable(false);
    	display.setContentType("text/html");
    	display.setPreferredSize(new Dimension(250, 145));
    	display.setMinimumSize(new Dimension(10, 10));
    	display.setBackground(new Color(0.94F, 0.94F, 0.94F));
    	display.setBorder(BorderFactory.createCompoundBorder(
    			BorderFactory.createMatteBorder(10,10,10,10, Color.WHITE), 
    			BorderFactory.createLoweredBevelBorder()));
        dispFrame.add(display);

    	optionsFrame.setLayout(new BoxLayout(optionsFrame, BoxLayout.Y_AXIS));
    	optionsFrame.setBackground(Color.WHITE);
        dispFrame.add(optionsFrame);
        
        redisplay();
    	
        JPanel buttonFrame = new JPanel();
        backButton = new JButton(back);
        backButton.setVerticalTextPosition(AbstractButton.CENTER);
        backButton.setHorizontalTextPosition(AbstractButton.CENTER); //aka LEFT, for left-to-right locales
        backButton.setMnemonic(KeyEvent.VK_A);
        backButton.setActionCommand(back);
 
        nextButton = new JButton(next);
        nextButton.setVerticalTextPosition(AbstractButton.CENTER);
        nextButton.setHorizontalTextPosition(AbstractButton.CENTER);
        nextButton.setMnemonic(KeyEvent.VK_Z);
        nextButton.setActionCommand(next);

        exitButton = new JButton(saveandexit);
        exitButton.setVerticalTextPosition(AbstractButton.CENTER);
        exitButton.setHorizontalTextPosition(AbstractButton.CENTER);
        exitButton.setMnemonic(KeyEvent.VK_Z);
        exitButton.setActionCommand(saveandexit);
  
        //Listen for actions on buttons 1 and 2.
        backButton.addActionListener(this);
        nextButton.addActionListener(this);
        exitButton.addActionListener(this);
 
        backButton.setToolTipText("Click this button to return to the previous item.");
        nextButton.setToolTipText("Click this button to skip to the next item.");
        exitButton.setToolTipText("Click this button to save the current document and exit.");
 
        //Add Components to this container, using the default FlowLayout.
        buttonFrame.add(backButton);
        buttonFrame.add(nextButton);
        buttonFrame.add(exitButton);
        
        add(dispFrame);
        add(buttonFrame);
    }
	
    private static void createAndShowGUI(final SimpleManualAnnotator sma) {
        //Create and set up the window.
        JFrame frame = new JFrame("GATE Simple Manual Annotator");
        frame.setPreferredSize(new Dimension(700, 450));
        frame.setMinimumSize(new Dimension(10, 10));
 
        //Create and set up the content pane.
        sma.setOpaque(true); //content panes must be opaque
        sma.setLayout(new BoxLayout(sma, BoxLayout.Y_AXIS));
        frame.setContentPane(sma);
 
        //Display the window.
        frame.pack();
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation(dim.width/2-frame.getSize().width/2, dim.height/2-frame.getSize().height/2);
        frame.setVisible(true);
        
        class MyWindowListener extends WindowAdapter {
    		public void windowClosing(WindowEvent ev) {
    			saveDoc(sma);
    			System.exit(0);
			}
    	}
        frame.addWindowListener(new MyWindowListener());
    }
    
    
    
    public static void main(String[] args) throws Exception {
    	Gate.init();
		gate.Utils.loadPlugin("Format_FastInfoset");
		
    	if(args.length!=2){
    		System.out.println("Usage: simpleManualAnnotator <config> <corpusDir>");
			System.exit(0);
    	} else {
			corpusdir = new File(args[1]);
			if(!corpusdir.isDirectory()){
				System.err.println(corpusdir.getAbsolutePath() + " is not a directory!");
				System.exit(0);
			} else {
				docsInDir = corpusdir.listFiles().length;
				System.out.println("Annotating " + docsInDir + " documents from "
						+ corpusdir.getAbsolutePath());
			}
    	}
    	
    	if(docsInDir<1){
			System.err.println("No documents to annotate!");
			System.exit(0);
    	}

    	SimpleManualAnnotator sma = new SimpleManualAnnotator(new File(args[0]));
    	createAndShowGUI(sma);
    }

	@Override
	public void actionPerformed(ActionEvent ev) {
		AnnotationSet outputAS = currentDoc.getAnnotations(config.outputASName);
		
		if (back.equals(ev.getActionCommand())) {
			prev();
	    } else if (next.equals(ev.getActionCommand())) {
			next();
	    } else if (saveandexit.equals(ev.getActionCommand())) {
			saveDoc(this);
			System.exit(0);
	    } else {
	    	currentAnnotationTask.updateDocument(ev);
			next();
	    }
		
		redisplay();
        revalidate();
        repaint();
	}
	
	public void next(){
		//If we are initializing or on the last annotation we need a new doc
        if(currentDocIndex==-1 || currentAnnIndex==mentionsInDoc-1){
			saveDoc(this);
        	int foundAnns = 0;
        	while(foundAnns==0){//Hunt for next doc with mentions
	    		if(currentDocIndex<corpusdir.listFiles().length-1){
	    			if(currentDoc!=null) Factory.deleteResource(currentDoc);
	    			this.currentDocIndex++;
	    			File thisdoc = corpusdir.listFiles()[currentDocIndex];
	    	   		try {
	    	   			currentDoc = Factory.newDocument(thisdoc.toURI().toURL());
	    	   		} catch (Exception e) {
	    	   			// TODO Auto-generated catch block
	    	   			e.printStackTrace();
	    	   		}
	    	   		
	    	   		//Having got the doc we set up the anns
	    	   		mentionList = currentDoc.getAnnotations(config.inputASName).get(config.mentionType).inDocumentOrder();
	    	   		mentionsInDoc = mentionList.size();
	    	   		currentAnnIndex = -1;
	    	   		if(mentionsInDoc>0){
	    	   			foundAnns = 1; //Exit while loop, as we are happy with doc
	    	   		}
	    		} else {
	    			return; //We got stuck on the last one so exit
	    		}
        	}
        }
        
        //So now we should be in a position to simply take the next mention ann
        //since we have made sure we are on a doc with at least one remaining mention.
        currentAnnIndex++;
        Annotation toDisplay = mentionList.get(currentAnnIndex);
        currentAnnotationTask = new AnnotationTask(toDisplay, config, currentDoc);
	}
	
	public void prev(){
		if(currentAnnIndex<1){ //We need to move back a doc
			saveDoc(this);
        	int foundAnns = 0;
        	while(foundAnns==0){//Hunt for preceding doc with mentions
	    		if(currentDocIndex>0){
	    			if(currentDoc!=null) Factory.deleteResource(currentDoc);
	    			this.currentDocIndex--;
	    			File thisdoc = corpusdir.listFiles()[currentDocIndex];
	    	   		try {
	    	   			currentDoc = Factory.newDocument(thisdoc.toURI().toURL());
	    	   		} catch (Exception e) {
	    	   			// TODO Auto-generated catch block
	    	   			e.printStackTrace();
	    	   		}
	    	   		
	    	   		//Having got the doc we set up the anns
	    	   		mentionList = currentDoc.getAnnotations(config.inputASName).get(config.mentionType).inDocumentOrder();
	    	   		mentionsInDoc = mentionList.size();
	    	   		currentAnnIndex = mentionsInDoc;
	    	   		if(mentionsInDoc>0){
	    	   			foundAnns = 1; //Exit while loop, as we are happy with doc
	    	   		}
	    		} else {
	    			return; //We got stuck on the first one so exit
	    		}
        	}
        }
        
        //So now we should be in a position to simply take the next mention ann
        //since we have made sure we are on a doc with at least one remaining mention.
        currentAnnIndex--;
        Annotation toDisplay = mentionList.get(currentAnnIndex);
        currentAnnotationTask = new AnnotationTask(toDisplay, config, currentDoc);
	}
	
	private void redisplay(){
		if(currentAnnotationTask==null){
			return;
		}
    	progress.setText(progressReport());
    	
    	int start = new Long(currentAnnotationTask.startOfMention-currentAnnotationTask.offset).intValue();
    	int end = new Long(currentAnnotationTask.endOfMention-currentAnnotationTask.offset).intValue();
    	String htmlStr = currentAnnotationTask.context.substring(0, start);
    	htmlStr = htmlStr + "<b><span style=\"background-color:#66CDAA\">";
    	htmlStr = htmlStr + currentAnnotationTask.context.substring(start, end);
    	htmlStr = htmlStr + "</span></b>";
    	htmlStr = htmlStr + currentAnnotationTask.context.substring(end, currentAnnotationTask.context.length());
    	display.setText(htmlStr);

    	//Remove any existing radio buttons from optionsFrame
    	while(optionsFrame.getComponentCount()>0){
    		optionsFrame.remove(optionsFrame.getComponentCount()-1);
    	}
    	optionGroup = new ButtonGroup();
    	
        for(int i=0;i<currentAnnotationTask.options.length;i++){
	        JRadioButton button = new JRadioButton(currentAnnotationTask.options[i]);
	        button.setMnemonic(i);
	        button.setActionCommand("option" + i);
	        button.setSelected(true);
	        optionGroup.add(button);
	        button.addActionListener(this);
	        if(currentAnnotationTask.indexOfSelected==i){
	        	button.setSelected(true);
	        }
	        button.setBackground(new Color(0.99F, 0.95F, 0.99F));
	        optionsFrame.add(button);
	    }
        if(config.includeNoneOfAbove){
	        JRadioButton button = new JRadioButton(AnnotationTask.noneofabove);
	        button.setMnemonic(KeyEvent.VK_9);
	        button.setActionCommand(AnnotationTask.noneofabove);
	        optionGroup.add(button);
	        button.addActionListener(this);
	        if(currentAnnotationTask.indexOfSelected==AnnotationTask.NONEOFABOVE){
	        	button.setSelected(true);
	        }
	        button.setBackground(new Color(0.99F, 0.95F, 0.99F));
	        optionsFrame.add(button);
        }
        if(config.includeSpurious){
	        JRadioButton button = new JRadioButton(AnnotationTask.spurious);
	        button.setMnemonic(KeyEvent.VK_0);
	        button.setActionCommand(AnnotationTask.spurious);
	        optionGroup.add(button);
	        button.addActionListener(this);
	        if(currentAnnotationTask.indexOfSelected==AnnotationTask.SPURIOUS){
	        	button.setSelected(true);
	        }
	        button.setBackground(new Color(0.99F, 0.95F, 0.99F));
	        optionsFrame.add(button);
        }
        JRadioButton button = new JRadioButton(AnnotationTask.undone);
        button.setMnemonic(KeyEvent.VK_0);
        button.setActionCommand(AnnotationTask.undone);
        optionGroup.add(button);
        button.addActionListener(this);
        if(currentAnnotationTask.indexOfSelected==AnnotationTask.UNDONE){
        	button.setSelected(true);
        }
        button.setBackground(Color.WHITE);
        optionsFrame.add(button);
	}
	
	private String progressReport(){
		return (currentDocIndex+1) + " of " + docsInDir + " docs, "
    			+ (currentAnnIndex+1) + " of " + mentionsInDoc + " annotations.";
	}
	
	private static void saveDoc(SimpleManualAnnotator sma){
		if(sma.currentDoc!=null){
			FileWriter thisdocfile = null;
			try {
				thisdocfile = new FileWriter(corpusdir.listFiles()[sma.currentDocIndex]);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(thisdocfile!=null){
				try {
					thisdocfile.write(sma.currentDoc.toXml());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				try {
					thisdocfile.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				System.out.println("Failed to access file " + sma.currentDocIndex
						+ " in " + corpusdir.getAbsolutePath() + " for writing!");
			}
		}
	}
}
