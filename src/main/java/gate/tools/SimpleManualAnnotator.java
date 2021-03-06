package gate.tools;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.DocumentExporter;
import gate.Factory;
import gate.Gate;
import gate.Utils;
import gate.creole.Plugin;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.ScrollPane;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import org.xhtmlrenderer.layout.FloatLayoutResult;

public class SimpleManualAnnotator extends JPanel implements ActionListener {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  /**
   * Create the GUI and show it. For thread safety, this method should be invoked from the
   * event-dispatching thread.
   */

  public enum Mode {
    OPTIONSFROMTYPEANDFEATURE, OPTIONSFROMFEATURE, OPTIONSFROMSTRING, OPTIONSFROMLISTANN,
  }

  static String backundone = "Last Undone";
  static String back = "Back";
  static String next = "Next";
  static String nextundone = "Next Undone";
  static String save = "Save";
  static String saveandexit = "Save and Exit";
  
  static DocumentExporter finfExporter;

  static JFrame frame = new JFrame("GATE Simple Manual Annotator");
  JButton lastUndoneButton, backButton, nextButton, undoneButton, saveButton, exitButton;
  JLabel progress;
  JEditorPane display = new JEditorPane();
  ButtonGroup optionGroup = new ButtonGroup();
  JPanel optionsFrame = new JPanel();
  JTextField note = new JTextField(10);
  JTextField nvTextField = new JTextField();


  //Set at init
  File[] corpus;
  Configuration config;

  //Globals for corpus navigation
  Document currentDoc;
  List<Annotation> mentionList;
  int currentDocIndex = -1;
  int currentAnnIndex = -1;
  int mentionsInDoc = -1;
  AnnotationTask currentAnnotationTask;

  public SimpleManualAnnotator(File conf, File[] corpus) {
    config = new Configuration(conf);
    this.corpus = corpus;
    next(true && config.undoneOnly);

    JPanel dispFrame = new JPanel();
    dispFrame.setLayout(new BoxLayout(dispFrame, BoxLayout.Y_AXIS));
    dispFrame.setBackground(Color.WHITE);

    JPanel progressFrame = new JPanel();
    progressFrame.setLayout(new BoxLayout(progressFrame, BoxLayout.LINE_AXIS));
    progressFrame.setPreferredSize(new Dimension(500, 47));
    progressFrame.setBorder(BorderFactory.createMatteBorder(5, 5, 5, 5, Color.WHITE));
    progressFrame.setBackground(Color.WHITE);
    progress = new JLabel();
    progress.setPreferredSize(new Dimension(400, 50));
    progress.setMinimumSize(new Dimension(10, 10));
    progressFrame.add(progress);
    dispFrame.add(progressFrame);
    
    display.setEditable(false);
    display.setContentType("text/html");
    display.setPreferredSize(new Dimension(500, 120));
    display.setMinimumSize(new Dimension(10, 10));
    display.setBackground(new Color(0.94F, 0.94F, 0.94F));
    display.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(10, 10, 10, 10, Color.WHITE),
            BorderFactory.createLoweredBevelBorder()));
    dispFrame.add(display);

    JPanel optionsContainer = new JPanel(new BorderLayout());
    optionsContainer.setBorder(BorderFactory.createMatteBorder(0, 25, 0, 0, Color.WHITE));
    optionsContainer.setBackground(Color.WHITE);
    optionsFrame.setLayout(new BoxLayout(optionsFrame, BoxLayout.Y_AXIS));
    optionsFrame.setBackground(Color.WHITE);
    optionsContainer.add(optionsFrame, BorderLayout.WEST);
    dispFrame.add(optionsContainer);

    // The entry field for a new value: only used if this option is enabled    
    if (config.includeNewValue) {
      JPanel newValuePanel = new JPanel();
      newValuePanel.setLayout(new BoxLayout(newValuePanel, BoxLayout.LINE_AXIS));
      newValuePanel.setPreferredSize(new Dimension(500, 40));
      newValuePanel.setBorder(BorderFactory.createMatteBorder(10, 10, 10, 10, Color.WHITE));
      newValuePanel.setBackground(Color.WHITE);
      JLabel vnLabel = new JLabel();
      vnLabel.setText("New Value: ");
      newValuePanel.add(vnLabel);
      nvTextField.setEditable(true);
      ScrollPane sp = new ScrollPane();
      sp.add(nvTextField);
      newValuePanel.add(sp);
      dispFrame.add(newValuePanel);
    }



    JPanel notePanel = new JPanel();
    notePanel.setLayout(new BoxLayout(notePanel, BoxLayout.LINE_AXIS));
    notePanel.setPreferredSize(new Dimension(500, 40));
    notePanel.setBorder(BorderFactory.createMatteBorder(10, 10, 10, 10, Color.WHITE));
    notePanel.setBackground(Color.WHITE);
    JLabel noteLabel = new JLabel();
    noteLabel.setText("Note: ");
    notePanel.add(noteLabel);
    ScrollPane sp = new ScrollPane();
    note.setEditable(true);
    sp.add(note);
    note.setBackground(new Color(0.94F, 0.94F, 0.94F));
    notePanel.add(sp);
    dispFrame.add(notePanel);

    
    redisplay();

    JPanel buttonFrame = new JPanel();
    lastUndoneButton = new JButton(backundone);
    lastUndoneButton.setVerticalTextPosition(AbstractButton.CENTER);
    lastUndoneButton.setHorizontalTextPosition(AbstractButton.CENTER); //aka LEFT, for left-to-right locales
    lastUndoneButton.setActionCommand(backundone);

    backButton = new JButton(back);
    backButton.setVerticalTextPosition(AbstractButton.CENTER);
    backButton.setHorizontalTextPosition(AbstractButton.CENTER); //aka LEFT, for left-to-right locales
    backButton.setActionCommand(back);

    nextButton = new JButton(next);
    nextButton.setVerticalTextPosition(AbstractButton.CENTER);
    nextButton.setHorizontalTextPosition(AbstractButton.CENTER);
    nextButton.setActionCommand(next);

    undoneButton = new JButton(nextundone);
    undoneButton.setVerticalTextPosition(AbstractButton.CENTER);
    undoneButton.setHorizontalTextPosition(AbstractButton.CENTER);
    undoneButton.setActionCommand(nextundone);

    saveButton = new JButton(save);
    saveButton.setVerticalTextPosition(AbstractButton.CENTER);
    saveButton.setHorizontalTextPosition(AbstractButton.CENTER);
    saveButton.setActionCommand(save);

    exitButton = new JButton(saveandexit);
    exitButton.setVerticalTextPosition(AbstractButton.CENTER);
    exitButton.setHorizontalTextPosition(AbstractButton.CENTER);
    exitButton.setActionCommand(saveandexit);

    //Listen for actions on buttons 1 and 2.
    lastUndoneButton.addActionListener(this);
    backButton.addActionListener(this);
    nextButton.addActionListener(this);
    undoneButton.addActionListener(this);
    saveButton.addActionListener(this);
    exitButton.addActionListener(this);

    lastUndoneButton.setToolTipText("Click this button to return to the last undone item.");
    backButton.setToolTipText("Click this button to return to the previous item.");
    nextButton.setToolTipText("Click this button to skip to the next item.");
    undoneButton.setToolTipText("Click this button to skip to the next undone item.");
    saveButton.setToolTipText("Click this button to save the current document.");
    exitButton.setToolTipText("Click this button to save the current document and exit.");

    //Add Components to this container, using the default FlowLayout.
    buttonFrame.add(lastUndoneButton);
    buttonFrame.add(backButton);
    buttonFrame.add(nextButton);
    buttonFrame.add(undoneButton);
    buttonFrame.add(saveButton);
    buttonFrame.add(exitButton);

    add(dispFrame);
    add(buttonFrame);

  }

  private static void createAndShowGUI(final SimpleManualAnnotator sma) {
    //Create and set up the window.
    frame.setPreferredSize(new Dimension(1400, 650));
    frame.setMinimumSize(new Dimension(50, 10));

    //Create and set up the content pane.
    sma.setOpaque(true); //content panes must be opaque
    sma.setLayout(new BoxLayout(sma, BoxLayout.Y_AXIS));
    frame.setContentPane(sma);

    //Display the window.
    frame.pack();
    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
    frame.setLocation(dim.width / 4 - frame.getSize().width / 2, dim.height / 4 - frame.getSize().height / 2);
    frame.setVisible(true);

    frame.addWindowListener(sma.new MyWindowListener());
    frame.addKeyListener(sma.new MyKeyListener());
    frame.setFocusable(true); // set focusable to true
    frame.requestFocusInWindow(); // request focus
  }

  class MyWindowListener extends WindowAdapter {

    @Override
    public void windowClosing(WindowEvent ev) {
      saveDoc();
      System.exit(0);
    }
  }

  class MyKeyListener extends KeyAdapter {

    @Override
    public void keyPressed(KeyEvent e) {
      switch (e.getKeyCode()) {
        case KeyEvent.VK_LEFT:
        case KeyEvent.VK_UP:
          act(back,"");
          break;
        case KeyEvent.VK_RIGHT:
        case KeyEvent.VK_DOWN:
          act(next,"");
          break;
        case KeyEvent.VK_Q:
          act(AnnotationTask.NONEOFABOVE_LABEL,"");
          break;
        case KeyEvent.VK_A:
          act(AnnotationTask.SPURIOUS_LABEL,"");
          break;
        case KeyEvent.VK_Z:
          act(AnnotationTask.UNDONE_LABEL,"");
          break;
        case KeyEvent.VK_1:
        case KeyEvent.VK_NUMPAD1:
          act("option0",""); //It expects index
          break;
        case KeyEvent.VK_2:
        case KeyEvent.VK_NUMPAD2:
          act("option1","");
          break;
        case KeyEvent.VK_3:
        case KeyEvent.VK_NUMPAD3:
          act("option2","");
          break;
        case KeyEvent.VK_4:
        case KeyEvent.VK_NUMPAD4:
          act("option3","");
          break;
        case KeyEvent.VK_5:
        case KeyEvent.VK_NUMPAD5:
          act("option4","");
          break;
        case KeyEvent.VK_6:
        case KeyEvent.VK_NUMPAD6:
          act("option5","");
          break;
        case KeyEvent.VK_7:
        case KeyEvent.VK_NUMPAD7:
          act("option6","");
          break;
        case KeyEvent.VK_8:
        case KeyEvent.VK_NUMPAD8:
          act("option7","");
          break;
        case KeyEvent.VK_9:
        case KeyEvent.VK_NUMPAD9:
          act("option8","");
          break;
      }
    }
  }

  public static void main(String[] args) throws Exception {
    Gate.init();
    Gate.getCreoleRegister().registerPlugin(new Plugin.Maven("uk.ac.gate.plugins","format-fastinfoset","8.5"));
    finfExporter = (DocumentExporter)Gate.getCreoleRegister()
            .get("gate.corpora.FastInfosetExporter")
            .getInstantiations().iterator().next();
            
    File[] corpus = new File[0];

    if (args.length != 2) {
      System.out.println("Usage: simpleManualAnnotator <config> <corpusDir>");
      System.exit(0);
    } else {
      File corpusdir = new File(args[1]);
      if (!corpusdir.isDirectory()) {
        System.err.println(corpusdir.getAbsolutePath() + " is not a directory!");
        System.exit(0);
      } else {
        corpus = corpusdir.listFiles();
        Arrays.sort(corpus);
        System.out.println("Annotating " + corpus.length + " documents from "
                + corpusdir.getAbsolutePath());
      }
    }

    if (corpus.length < 1) {
      System.err.println("No documents to annotate!");
      System.exit(0);
    }

    SimpleManualAnnotator sma = new SimpleManualAnnotator(new File(args[0]), corpus);
    createAndShowGUI(sma);
  }

  @Override
  public void actionPerformed(ActionEvent ev) {
    act(ev.getActionCommand(),ev.getSource());
  }

  public void act(String what, Object evSource) {

    if (backundone.equals(what)) {
      currentAnnotationTask.updateNote(this.note.getText());
      prev(true);
    } else if (back.equals(what)) {
      currentAnnotationTask.updateNote(this.note.getText());
      prev(false);
    } else if (next.equals(what)) {
      currentAnnotationTask.updateNote(this.note.getText());
      next(false);
    } else if (nextundone.equals(what)) {
      currentAnnotationTask.updateNote(this.note.getText());
      next(true);
    } else if (save.equals(what)) {
      currentAnnotationTask.updateNote(this.note.getText());
      saveDoc();
    } else if (saveandexit.equals(what)) {
      currentAnnotationTask.updateNote(this.note.getText());
      saveDoc();
      System.exit(0);
    } else {
      String newValue = "";
      if(what.equals("<NEWVALUE>")) {
        newValue = ((RadioButtonForTextEntry)evSource).getTextField().getText();
        ((RadioButtonForTextEntry)evSource).getTextField().setText("");
      }
      int error = currentAnnotationTask.updateDocument(what,newValue);
      currentAnnotationTask.updateNote(note.getText());
      if (error != -1 && config.autoadvance) {
        next(false);
      }
    }

    redisplay();
    revalidate();
    repaint();
    frame.requestFocusInWindow();
  }

  public void updateToNext(boolean skipCompleteDocs) {
    //If we are initializing or on the last annotation we need a new doc
    if (currentDocIndex == -1 || currentAnnIndex == mentionsInDoc - 1) {
      saveDoc();
      int foundAnns = 0;
      while (foundAnns == 0) {//Hunt for next doc with mentions
        if (currentDocIndex < corpus.length - 1) {
          if (currentDoc != null) {
            Factory.deleteResource(currentDoc);
          }
          this.currentDocIndex++;
          File thisdoc = corpus[currentDocIndex];
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

          AnnotationSet doneAnns = currentDoc.getAnnotations(config.outputASName).get(config.mentionType);
          boolean isComplete = false;
          if (mentionList.size() == doneAnns.size()) {
            isComplete = true;
          }

          if ((!skipCompleteDocs && mentionsInDoc > 0) || (skipCompleteDocs && !isComplete && mentionsInDoc > 0)) {
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

  public void next(boolean undoneOnly) {
    if (!undoneOnly) {
      updateToNext(false);
    } else {
      int prevAnnIndex = currentAnnIndex;
      updateToNext(true);
      AnnotationSet doneAnns = currentDoc.getAnnotations(config.outputASName).get(config.mentionType);
      Annotation thisAnn = mentionList.get(currentAnnIndex);
      AnnotationSet isitdone = Utils.getCoextensiveAnnotations(doneAnns, thisAnn);
      while (isitdone.size() != 0 && currentAnnIndex != prevAnnIndex) {
        prevAnnIndex = currentAnnIndex;
        updateToNext(true);
        doneAnns = currentDoc.getAnnotations(config.outputASName).get(config.mentionType);
        thisAnn = mentionList.get(currentAnnIndex);
        isitdone = Utils.getCoextensiveAnnotations(doneAnns, thisAnn);
      }
    }
  }

  public void updateToPrev(boolean skipCompleteDocs) {
    if (currentAnnIndex < 1) { //We need to move back a doc
      saveDoc();
      int foundAnns = 0;
      while (foundAnns == 0) {//Hunt for preceding doc with mentions
        if (currentDocIndex > 0) {
          if (currentDoc != null) {
            Factory.deleteResource(currentDoc);
          }
          this.currentDocIndex--;
          File thisdoc = corpus[currentDocIndex];
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

          AnnotationSet doneAnns = currentDoc.getAnnotations(config.outputASName).get(config.mentionType);
          boolean isComplete = false;
          if (mentionList.size() == doneAnns.size()) {
            isComplete = true;
          }

          if ((!skipCompleteDocs && mentionsInDoc > 0) || (skipCompleteDocs && !isComplete && mentionsInDoc > 0)) {
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

  public void prev(boolean undoneOnly) {
    if (!undoneOnly) {
      updateToPrev(false);
    } else {
      int prevAnnIndex = currentAnnIndex;
      updateToPrev(true);
      AnnotationSet doneAnns = currentDoc.getAnnotations(config.outputASName).get(config.mentionType);
      Annotation thisAnn = mentionList.get(currentAnnIndex);
      AnnotationSet isitdone = Utils.getCoextensiveAnnotations(doneAnns, thisAnn);
      while (isitdone.size() == 1 && currentAnnIndex != prevAnnIndex) {
        prevAnnIndex = currentAnnIndex;
        updateToPrev(true);
        doneAnns = currentDoc.getAnnotations(config.outputASName).get(config.mentionType);
        thisAnn = mentionList.get(currentAnnIndex);
        isitdone = Utils.getCoextensiveAnnotations(doneAnns, thisAnn);
      }
    }
  }

  private void redisplay() {
    if (currentAnnotationTask == null) {
      return;
    }
    progress.setText(progressReport());
    
    nvTextField.setText("");

    int start = new Long(currentAnnotationTask.startOfMention - currentAnnotationTask.offset).intValue();
    if (start < 0) {
      start = 0;
    }

    int end = new Long(currentAnnotationTask.endOfMention - currentAnnotationTask.offset).intValue();
    if (end > currentAnnotationTask.context.length()) {
      end = currentAnnotationTask.context.length();
    }

    String htmlStr = currentAnnotationTask.context.substring(0, start);
    htmlStr = htmlStr + "<b><span style=\"background-color:#66CDAA\">";
    htmlStr = htmlStr + currentAnnotationTask.context.substring(start, end);
    htmlStr = htmlStr + "</span></b>";
    htmlStr = htmlStr + currentAnnotationTask.context.substring(end, currentAnnotationTask.context.length());
    display.setText(htmlStr);

    note.setText(currentAnnotationTask.note);

    //Remove any existing radio buttons from optionsFrame
    while (optionsFrame.getComponentCount() > 0) {
      optionsFrame.remove(optionsFrame.getComponentCount() - 1);
    }
    optionGroup = new ButtonGroup();
    
    // TODO: JP: always set the note field to editable!
    // was previously only done in // HERE1
    note.setBackground(Color.WHITE);
    note.setEditable(true);
    note.setText(currentAnnotationTask.note);

    for (int i = 0; i < currentAnnotationTask.options.length; i++) {
      JRadioButton button = new JRadioButton(i + 1 + ": " + currentAnnotationTask.options[i]);
      button.setActionCommand("option" + i);
      optionGroup.add(button);
      button.addActionListener(this);
      if (currentAnnotationTask.indexOfSelected == i) {
        // HERE1
        button.setSelected(true);
      }
      button.setBackground(new Color(0.99F, 0.95F, 0.99F));
      optionsFrame.add(button);
    }
    if (config.includeNoneOfAbove) {
      JRadioButton button = new JRadioButton("Q: " + AnnotationTask.NONEOFABOVE_LABEL);
      button.setActionCommand(AnnotationTask.NONEOFABOVE_LABEL);
      optionGroup.add(button);
      button.addActionListener(this);
      if (currentAnnotationTask.indexOfSelected == AnnotationTask.NONEOFABOVE) {
        button.setSelected(true);
        // HERE1
      }
      button.setBackground(new Color(0.99F, 0.95F, 0.99F));
      optionsFrame.add(button);
    }
    if (config.includeSpurious) {
      JRadioButton button = new JRadioButton("A: " + AnnotationTask.SPURIOUS_LABEL);
      button.setActionCommand(AnnotationTask.SPURIOUS_LABEL);
      optionGroup.add(button);
      button.addActionListener(this);
      if (currentAnnotationTask.indexOfSelected == AnnotationTask.SPURIOUS) {
        button.setSelected(true);
        // HERE1
      }
      button.setBackground(new Color(0.99F, 0.95F, 0.99F));
      optionsFrame.add(button);
    }
    if (config.includeNIL) {
      JRadioButton button = new JRadioButton("X: " + AnnotationTask.NIL_LABEL);
      button.setActionCommand(AnnotationTask.NIL_LABEL);
      optionGroup.add(button);
      button.addActionListener(this);
      if (currentAnnotationTask.indexOfSelected == AnnotationTask.NIL) {
        button.setSelected(true);
        // HERE1
      }
      button.setBackground(new Color(0.99F, 0.95F, 0.99F));
      optionsFrame.add(button);
    }
    if (config.includeNewValue) {
      JRadioButton button = new RadioButtonForTextEntry("N: new ",nvTextField);
      button.setActionCommand(AnnotationTask.NEWVALUE_LABEL);
      optionGroup.add(button);
      button.addActionListener(this);
      button.setBackground(new Color(0.99F, 0.95F, 0.99F));
      optionsFrame.add(button);
      if(currentAnnotationTask != null && !currentAnnotationTask.newValue.isEmpty()) {
        nvTextField.setText(currentAnnotationTask.newValue);
        button.setSelected(true);
      }
    }
    JRadioButton button = new JRadioButton("Z: " + AnnotationTask.UNDONE_LABEL);
    button.setActionCommand(AnnotationTask.UNDONE_LABEL);
    optionGroup.add(button);
    button.addActionListener(this);
    if (currentAnnotationTask.indexOfSelected == AnnotationTask.UNDONE) {
      button.setSelected(true);
      // JP: This originally prevented the note to be editable, probably to avoid
      // writing a note to an output annotation if the location is left undone.
      // However, since selecting the button will immediately advance, there was 
      // no way to add a note to a new annotatin.
      // So instead we always show the note and just ignore it when the undone option is
      // selected      
      // this.note.setBackground(Color.LIGHT_GRAY);
      // this.note.setText("");
      // this.note.setEditable(false);
    }
    button.setBackground(Color.WHITE);
    optionsFrame.add(button);
  }

  private String progressReport() {
    return currentDoc.getName() + ": " + (currentDocIndex + 1) + " of " + corpus.length + " docs, "
            + (currentAnnIndex + 1) + " of " + mentionsInDoc + " annotations.";
  }

  private void saveDoc() {
    if (currentDoc != null) {
      File docFile = corpus[currentDocIndex];
      try {
        if(docFile.toString().endsWith(".finf")) {
          finfExporter.export(currentDoc, docFile, Factory.newFeatureMap());
          System.out.println("Document saved in FINF format: "+docFile.getAbsolutePath());
        } else {
          gate.corpora.DocumentStaxUtils.writeDocument(currentDoc,docFile);
          System.out.println("Document saved in XML format: "+docFile.getAbsolutePath());
        }
      } catch (Exception e) {
        System.err.println("Error when trying to save document "+docFile.getAbsolutePath());
        e.printStackTrace(System.err);
      }
    }
  }
  
  // A radio button that also holds a reference to a text entry field so
  // the content of that field can be accessed if we get an event from the radio button
  public static class RadioButtonForTextEntry extends JRadioButton {
    private JTextField textField;
    public RadioButtonForTextEntry(String text, JTextField tf) {
      super(text);
      textField = tf;
    }
    public JTextField getTextField() { return textField; }    
  }

  
}

