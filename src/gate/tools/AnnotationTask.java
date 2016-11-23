package gate.tools;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Utils;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class AnnotationTask {

  static int NONEOFABOVE = 10001;
  static int SPURIOUS = 10002;
  static int UNDONE = 10003;

  static String noneofabove = "<NONE OF ABOVE>";
  static String spurious = "<SPURIOUS>";
  static String undone = "<NOT DONE>";

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
        if (prev != null && ((String) prev).equals(noneofabove)) {
          indexOfSelected = NONEOFABOVE;
        } else if (prev != null && prev.equals(spurious)) {
          indexOfSelected = SPURIOUS;
        } else if (prev != null && prev.equals(undone)) {
          indexOfSelected = UNDONE;
        }
      }
    }

    mention = thisAnn;

    //Context
    Annotation contextAnn = Utils.getOverlappingAnnotations(
            inputAS, thisAnn, config.contextType).iterator().next();
    context = Utils.stringFor(currentDoc, contextAnn);
    offset = contextAnn.getStartNode().getOffset();
    startOfMention = thisAnn.getStartNode().getOffset();
    endOfMention = thisAnn.getEndNode().getOffset();

    //Options
    switch (config.mode) {
      case OPTIONSFROMTYPEANDFEATURE:
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
          // TODO: could add arrays here ...
          // } else if (featureValue instanceof String[])   
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
      case OPTIONSFROMTYPEANDFEATURE:
        if (AnnotationTask.spurious.equals(action) && config.includeSpurious) {
          fm.put(config.optionsFeat, AnnotationTask.spurious);
          Utils.addAnn(outputAS, mention, config.mentionType, fm);
          this.indexOfSelected = AnnotationTask.SPURIOUS;
        } else if (AnnotationTask.noneofabove.equals(action) && config.includeNoneOfAbove) {
          fm.put(config.optionsFeat, AnnotationTask.noneofabove);
          Utils.addAnn(outputAS, mention, config.mentionType, fm);
          this.indexOfSelected = AnnotationTask.NONEOFABOVE;
        } else if (AnnotationTask.undone.equals(action)) {
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
        if (AnnotationTask.spurious.equals(action)) {
          fm.put(config.outputFeat, AnnotationTask.spurious);
          Utils.addAnn(outputAS, mention, config.mentionType, fm);
          this.indexOfSelected = AnnotationTask.SPURIOUS;
        } else if (AnnotationTask.noneofabove.equals(action)) {
          fm.put(config.outputFeat, AnnotationTask.noneofabove);
          fm.put(noteType, this.note);
          Utils.addAnn(outputAS, mention, config.mentionType, fm);
          this.indexOfSelected = AnnotationTask.NONEOFABOVE;
        } else if (AnnotationTask.undone.equals(action)) {
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
        if (AnnotationTask.spurious.equals(action)) {
          fm.put(config.outputFeat, AnnotationTask.spurious);
          Utils.addAnn(outputAS, mention, config.mentionType, fm);
          this.indexOfSelected = AnnotationTask.SPURIOUS;
        } else if (AnnotationTask.noneofabove.equals(action)) {
          fm.put(config.outputFeat, AnnotationTask.noneofabove);
          fm.put(noteType, this.note);
          Utils.addAnn(outputAS, mention, config.mentionType, fm);
          this.indexOfSelected = AnnotationTask.NONEOFABOVE;
        } else if (AnnotationTask.undone.equals(action)) {
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
