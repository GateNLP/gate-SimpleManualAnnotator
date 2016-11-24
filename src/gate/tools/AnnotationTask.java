package gate.tools;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AnnotationTask {

  static final int NONEOFABOVE = 10001;
  static final int SPURIOUS = 10002;
  static final int UNDONE = 10003;
  static final int NIL = 10004;

  static final String NONEOFABOVE_LABEL = "<NONE OF ABOVE>";
  static final String SPURIOUS_LABEL = "<SPURIOUS>";
  static final String UNDONE_LABEL = "<NOT DONE>";
  static final String NIL_LABEL = "<NIL>";
  
  static final String NIL_VALUE = "";

  static String noteType = "annotation-note";

  private Document currentDoc;
  private Configuration config;

  Annotation mention;
  String context;
  long offset; //Relative to which start and end of mention are given
  long startOfMention;
  long endOfMention;
  String[] options;
  Object[] optionsObjects;
  Annotation previouslySelected = null;
  int indexOfSelected = UNDONE;
  String note;

  AnnotationTask(Annotation thisAnn, Configuration config, Document currentDoc) {
    this.config = config;
    this.currentDoc = currentDoc;
    AnnotationSet inputAS = currentDoc.getAnnotations(config.inputASName);
    AnnotationSet outputAS = currentDoc.getAnnotations(config.outputASName);
    AnnotationSet previous = Utils.getCoextensiveAnnotations(outputAS, thisAnn);
    Object prev = null;

    if (previous.size() == 1) {
      previouslySelected = Utils.getOnlyAnn(previous);

      if (previouslySelected.getFeatures().get(noteType) != null) {
        note = previouslySelected.getFeatures().get(noteType).toString();
      }

      if (config.mode == SimpleManualAnnotator.Mode.OPTIONSFROMTYPEANDFEATURE) {
        prev = previouslySelected.getFeatures().get(config.optionsFeat);
      } else {
        prev = previouslySelected.getFeatures().get(config.outputFeat);
      }
      if (prev instanceof String) {
        if (prev != null && ((String) prev).equals(NONEOFABOVE_LABEL)) {
          indexOfSelected = NONEOFABOVE;
        } else if (prev != null && prev.equals(SPURIOUS_LABEL)) {
          indexOfSelected = SPURIOUS;
        } else if (prev != null && prev.equals(UNDONE_LABEL)) {
          indexOfSelected = UNDONE;
        }
      }
    }

    mention = thisAnn;

    //Context
    if(config.contextType == null || config.contextType.isEmpty()) {
      context = currentDoc.getContent().toString();
      offset = 0;
    } else {
      Annotation contextAnn = Utils.getOverlappingAnnotations(
            inputAS, thisAnn, config.contextType).iterator().next();
      context = Utils.stringFor(currentDoc, contextAnn);
      offset = contextAnn.getStartNode().getOffset();
    }
    
    startOfMention = thisAnn.getStartNode().getOffset();
    endOfMention = thisAnn.getEndNode().getOffset();

    //Options
    switch (config.mode) {
      case OPTIONSFROMLISTANN:
        List<Integer> ids = (List<Integer>)thisAnn.getFeatures().get("ids");
        List<Annotation> optAnns = new ArrayList<Annotation>(ids.size());
        for(int id : ids) {
          optAnns.add(inputAS.get(id));
        }
        Iterator<Annotation> itanns = optAnns.iterator();
        this.options = new String[optAnns.size()];
        optionsObjects = new Annotation[optAnns.size()];
        int indexA = 0;
        while (itanns.hasNext()) {
          Annotation optan = itanns.next();
          String feat = optan.getFeatures().get(config.optionsFeat).toString();
          this.options[indexA] = feat;
          this.optionsObjects[indexA] = optan;
          if (prev != null && feat.equals((String) prev)) {
            indexOfSelected = indexA;
          }
          indexA++;
        }
        break;
      case OPTIONSFROMTYPEANDFEATURE:
        // get the list of annotation ids from this annotation 
        AnnotationSet optans = Utils.getCoextensiveAnnotations(inputAS, thisAnn, config.optionsType);
        Iterator<Annotation> it = optans.iterator();
        this.options = new String[optans.size()];
        optionsObjects = new Annotation[optans.size()];
        int index = 0;
        while (it.hasNext()) {
          Annotation optan = it.next();
          String feat = optan.getFeatures().get(config.optionsFeat).toString();
          this.options[index] = feat;
          this.optionsObjects[index] = optan;
          if (prev != null && feat.equals((String) prev)) {
            indexOfSelected = index;
          }
          index++;
        }
        break;
      case OPTIONSFROMFEATURE:
        Object featureValue = thisAnn.getFeatures().get(config.optionsFeat);
        // TODO: is this what we want?
        // if an annotation does not have the feature, log an error
        if (featureValue != null) {
          if (featureValue instanceof Iterable<?>) {
            Iterable<Object> iterable = (Iterable<Object>) featureValue;
            List<Object> asList = new ArrayList<Object>();
            Iterator<Object> itit = iterable.iterator();
            while (itit.hasNext()) {
              asList.add(itit.next());
            }
            this.optionsObjects = new Object[asList.size()];
            this.options = new String[asList.size()];
            int opindex = 0;
            for (Object whateveritis : asList) {
              //Object whateveritis = opit.next();
              this.optionsObjects[opindex] = whateveritis;
              this.options[opindex] = whateveritis.toString();
              if (prev != null && whateveritis.equals(prev)) {
                indexOfSelected = opindex;
              }
              opindex++;
            }
          } else if(featureValue instanceof String) {
            // if the featureValue is a string and the listSeparator is specified in the config,
            // split the String 
            if(config.listSeparator != null) {
              String values[] = ((String)featureValue).split(config.listSeparator,-1);
              int opindex = 0;
              for (String whateveritis : values) {
                this.optionsObjects[opindex] = whateveritis;
                this.options[opindex] = whateveritis;
                if (prev != null && whateveritis.equals(prev)) {
                  indexOfSelected = opindex;
                }
                opindex++;
              }
            } else {
              System.err.println("Feature is a String but no listSeparator " + currentDoc.getName() + " annotation " + thisAnn);              
            }
          } else if (featureValue instanceof String[]) {
            int opindex = 0;
            for (String whateveritis : ((String[])featureValue)) {
              this.optionsObjects[opindex] = whateveritis;
              this.options[opindex] = whateveritis;
              if (prev != null && whateveritis.equals(prev)) {
                indexOfSelected = opindex;
              }
              opindex++;
            }
            
          } else { // not a supported type
            // log this as an error
            System.err.println("Feature is not an Iterable in document " + currentDoc.getName() + " annotation " + thisAnn);
          }
        } else {
          System.err.println("Feature is null in document " + currentDoc.getName() + " annotation " + thisAnn);
        }
        break;
      case OPTIONSFROMSTRING:
        this.options = config.options;
        for (int j = 0; j < this.options.length; j++) {
          if (this.options[j].equals(prev)) {
            indexOfSelected = j;
          }
        }
        break;
    }
  }

  public int updateDocument(String action) {
    AnnotationSet outputAS = currentDoc.getAnnotations(config.outputASName);

    //Always start by removing whatever is there
    if (previouslySelected != null) {
      outputAS.remove(previouslySelected);
    }
    //To be sure ..
    outputAS.removeAll(Utils.getCoextensiveAnnotations(outputAS, mention));

    FeatureMap fm = Factory.newFeatureMap();

    switch (config.mode) {
      case OPTIONSFROMLISTANN:
        if (AnnotationTask.SPURIOUS_LABEL.equals(action) && config.includeSpurious) {
          fm.put(config.optionsFeat, AnnotationTask.SPURIOUS_LABEL);
          Utils.addAnn(outputAS, mention, config.mentionType, fm);
          this.indexOfSelected = AnnotationTask.SPURIOUS;
        } else if (AnnotationTask.NIL_LABEL.equals(action)) {
          fm.put(config.outputFeat, AnnotationTask.NIL_VALUE);
          Utils.addAnn(outputAS, mention, config.mentionType, fm);
        } else if (AnnotationTask.NONEOFABOVE_LABEL.equals(action) && config.includeNoneOfAbove) {
          fm.put(config.optionsFeat, AnnotationTask.NONEOFABOVE_LABEL);
          Utils.addAnn(outputAS, mention, config.mentionType, fm);
          this.indexOfSelected = AnnotationTask.NONEOFABOVE;
        } else if (AnnotationTask.UNDONE_LABEL.equals(action)) {
          //Nothing to do, we already removed it
          this.indexOfSelected = AnnotationTask.UNDONE;
        } else { //We have a potential option
          int opt = new Integer(action.substring(6)).intValue();
          if (opt >= 0 && opt < optionsObjects.length) {
            Annotation toAdd = (Annotation) optionsObjects[opt];
            fm.putAll(toAdd.getFeatures());
            Utils.addAnn(outputAS, mention, config.mentionType, fm);
            this.indexOfSelected = opt;
          } else {
            System.out.println("Ignoring invalid option.");
            return -1;
          }
        }
        break;
      case OPTIONSFROMTYPEANDFEATURE:
        if (AnnotationTask.SPURIOUS_LABEL.equals(action) && config.includeSpurious) {
          fm.put(config.optionsFeat, AnnotationTask.SPURIOUS_LABEL);
          Utils.addAnn(outputAS, mention, config.mentionType, fm);
          this.indexOfSelected = AnnotationTask.SPURIOUS;
        } else if (AnnotationTask.NIL_LABEL.equals(action)) {
          fm.put(config.outputFeat, AnnotationTask.NIL_VALUE);
          Utils.addAnn(outputAS, mention, config.mentionType, fm);
        } else if (AnnotationTask.NONEOFABOVE_LABEL.equals(action) && config.includeNoneOfAbove) {
          fm.put(config.optionsFeat, AnnotationTask.NONEOFABOVE_LABEL);
          Utils.addAnn(outputAS, mention, config.mentionType, fm);
          this.indexOfSelected = AnnotationTask.NONEOFABOVE;
        } else if (AnnotationTask.UNDONE_LABEL.equals(action)) {
          //Nothing to do, we already removed it
          this.indexOfSelected = AnnotationTask.UNDONE;
        } else { //We have a potential option
          int opt = new Integer(action.substring(6)).intValue();
          if (opt >= 0 && opt < optionsObjects.length) {
            Annotation toAdd = (Annotation) optionsObjects[opt];
            fm.putAll(toAdd.getFeatures());
            Utils.addAnn(outputAS, mention, config.mentionType, fm);
            this.indexOfSelected = opt;
          } else {
            System.out.println("Ignoring invalid option.");
            return -1;
          }
        }
        break;
      case OPTIONSFROMFEATURE:
        if (AnnotationTask.SPURIOUS_LABEL.equals(action)) {
          fm.put(config.outputFeat, AnnotationTask.SPURIOUS_LABEL);
          Utils.addAnn(outputAS, mention, config.mentionType, fm);
          this.indexOfSelected = AnnotationTask.SPURIOUS;
        } else if (AnnotationTask.NIL_LABEL.equals(action)) {
          fm.put(config.outputFeat, AnnotationTask.NIL_VALUE);
          Utils.addAnn(outputAS, mention, config.mentionType, fm);
        } else if (AnnotationTask.NONEOFABOVE_LABEL.equals(action)) {
          fm.put(config.outputFeat, AnnotationTask.NONEOFABOVE_LABEL);
          fm.put(noteType, this.note);
          Utils.addAnn(outputAS, mention, config.mentionType, fm);
          this.indexOfSelected = AnnotationTask.NONEOFABOVE;
        } else if (AnnotationTask.UNDONE_LABEL.equals(action)) {
          //Nothing to do, we already removed it
          this.indexOfSelected = AnnotationTask.UNDONE;
        } else { //We have an option
          int opt = new Integer(action.substring(6)).intValue();
          fm.put(config.outputFeat, this.optionsObjects[opt]);
          Utils.addAnn(outputAS, mention, config.mentionType, fm);
          this.indexOfSelected = opt;
        }
        break;
      case OPTIONSFROMSTRING:
        if (AnnotationTask.SPURIOUS_LABEL.equals(action)) {
          fm.put(config.outputFeat, AnnotationTask.SPURIOUS_LABEL);
          Utils.addAnn(outputAS, mention, config.mentionType, fm);
          this.indexOfSelected = AnnotationTask.SPURIOUS;
        } else if (AnnotationTask.NIL_LABEL.equals(action)) {
          fm.put(config.outputFeat, AnnotationTask.NIL_VALUE);
          Utils.addAnn(outputAS, mention, config.mentionType, fm);
        } else if (AnnotationTask.NONEOFABOVE_LABEL.equals(action)) {
          fm.put(config.outputFeat, AnnotationTask.NONEOFABOVE_LABEL);
          fm.put(noteType, this.note);
          Utils.addAnn(outputAS, mention, config.mentionType, fm);
          this.indexOfSelected = AnnotationTask.NONEOFABOVE;
        } else if (AnnotationTask.UNDONE_LABEL.equals(action)) {
          //Nothing to do, we already removed it
          this.indexOfSelected = AnnotationTask.UNDONE;
        } else { //We have an option
          int opt = new Integer(action.substring(6)).intValue();
          fm.put(config.outputFeat, this.options[opt]);
          Utils.addAnn(outputAS, mention, config.mentionType, fm);
          this.indexOfSelected = opt;
        }
        break;
    }
    return 1;
  }

  public void updateNote(String note) {
    this.note = note;
    AnnotationSet outputAS = currentDoc.getAnnotations(config.outputASName);
    AnnotationSet anns = Utils.getCoextensiveAnnotations(outputAS, mention);
    if (anns.size() > 0) {
      Annotation ann = anns.iterator().next();
      ann.getFeatures().put(noteType, this.note);
    }
  }
}
