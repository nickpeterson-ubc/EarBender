package com.dslproject.music;

import com.dslproject.ast.declarations.Note;
import com.dslproject.ast.declarations.Variable;
import com.dslproject.ui.Main;
import org.apache.maven.shared.utils.StringUtils;
import org.jfugue.pattern.Pattern;
import org.jfugue.pattern.PatternProducer;
import org.jfugue.pattern.Token;
import org.jfugue.player.Player;
import org.jfugue.rhythm.Rhythm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Music {

    Logger logger = LoggerFactory.getLogger(Main.class);
    Rhythm rhythm = new Rhythm();
    Player player = new Player();
    List<Pattern> patternList = new ArrayList<Pattern>();
    private double rhythmMeasures = 0;

    /**
     * Constructor
     */
    public Music(){

    }

//    /**
//     * Add a patten to be played later
//     *
//     * @param musicVar     MusicVar object
//     * @param channel      channel to play the MusicVar
//     */
//    public void addPattern(MusicVar musicVar, int channel){
//
//        // I think there is a bug in jfugue on setting the tempo
//        // for multiple patterns on multiple channel.
//        // It's related how they put to the order of the pattern string when we call pattern.setTempo(tempo);.
//        // To resolve this issue, we will avoid using methods like pattern.setTempo. And we build the pattern
//        // string manually.
//        // https://stackoverflow.com/questions/19673293/jfugue-not-changing-tempo-in-the-middle-of-a-song-with-2-voices
//
////        StringBuilder resultNotes = new StringBuilder();
////        for(int i=0; i<loopTimes; i++){
////            resultNotes.append(" ").append(notes);
////        }
////
////        Pattern pattern = new Pattern(resultNotes.toString());
////        pattern.setVoice(channel);
////        pattern.setTempo(tempo);
////        pattern.setInstrument(instrument);
////
////        logger.info("pattern:  " + pattern.toString());
////
////        patternList.add(pattern);
//
////        for(int i=0; i<loopTimes; i++){
////            patternList.add(pattern);
////        }
//
//        String patternStr = "";
//
//        patternStr += "V" + channel;
//        patternStr += " " + "T" + musicVar.getTempo();
//        patternStr += " " + "I[" + musicVar.getInstrument() + "]";
//
//        StringBuilder resultNotes = new StringBuilder();
//        for(int i=0; i<musicVar.getLoopTimes(); i++){
//            resultNotes.append(" ").append(musicVar.getNoteStr());
//        }
//
//        patternStr += " " + resultNotes.toString();
//
//        Pattern pattern = new Pattern(patternStr);
//        patternList.add(pattern);
//
//    }
//
//    /**
//     * Add a music layer to be played later
//     *
//     * @param musicLayer     a musicLayer object
//     */
//    public void addMusicLayer(MusicLayer musicLayer){
//
//        MusicVar[] musicVars = musicLayer.getMusicVars();
//        int channel = musicLayer.getChannel();
//
//        for (MusicVar musicVar : musicVars) {
//            String patternStr = "";
//
//            patternStr += "V" + channel;
//            patternStr += " " + "T" + musicVar.getTempo();
//            patternStr += " " + "I[" + musicVar.getInstrument() + "]";
//
//            StringBuilder resultNotes = new StringBuilder();
//            for (int j = 0; j < musicVar.getLoopTimes(); j++) {
//                resultNotes.append(" ").append(musicVar.getNoteStr());
//            }
//
//            patternStr += " " + resultNotes.toString();
//
//            Pattern pattern = new Pattern(patternStr);
//            patternList.add(pattern);
//        }
//
//    }

    /**
     * Add a music variable to be played later
     *
     * @param  var
     * @param  channel
     * @param  rest      true if we want to play all note as rest notes
     */
    public void addMusicVar(Variable var, int channel, boolean rest){

        String patternStr = "";
        String noteString = var.getNotesString();
        if(rest){
            noteString = var.getRestNoteString();
        }

        List<Note> notes = var.getNotes();

        patternStr += "V" + channel;
        patternStr += " " + "T" + var.getTempo();
        patternStr += " " + "I[" + var.getInstrument() + "]";
        patternStr += " " + noteString;

        Pattern pattern = new Pattern(patternStr);
        patternList.add(pattern);
    }

    /**
     * Add a list of music variable to be played later
     *
     * @param  lists
     * @param  channel
     * @param  rest      true if we want to play all note as rest notes
     */
    public void addMusicVarList(List<Variable> lists, int channel, boolean rest){
        for (Variable var: lists) {
            addMusicVar(var, channel, rest);
        }
    }

    /**
     * Add a layer to the rhythm
     * @param layer A string of rhythm e.x. "..X...X...X...XO"
     */
    public void addRhythmLayer(String layer){
        rhythm.addLayer(layer);
    }

    /**
     * Set the length of the rhythm
     * @param length the length of the rhythm
     */
    public void setRhythmLength(double length){
        this.rhythmMeasures = length;
        rhythm.setLength((int) Math.ceil(length));
    }

    /**
     * Play all the defined patterns and rhythm
     */
    public void playMusic(){
        trimRhythmPattern();
        patternList.add(rhythm.getPattern());
        Pattern[] patternArray = patternList.toArray(new Pattern[0]);
        logger.info("Music content:  " + StringUtils.join(patternArray, "\n"));
        player.play(patternList.toArray(patternArray));
    }

    // This method is trimming the rhythm pattern so the final measure does not continue
    // longer than channel 0. It's fairly complicated, there might be a simpler algorithm out there.
    private void trimRhythmPattern() {
        final double ONE_SIXTEENTH = .0625;
        // Calculate the number of beats to 'trim' off the end
        // Divide by 1/16 because there are 16 'sixteenth' in a measure
        // and layers are defined in sixteenth note increments
        final int trim = (int) ((rhythm.getLength() - this.rhythmMeasures) / ONE_SIXTEENTH);
        final List<String> layers = rhythm.getLayers();
        if (layers.size() > 0) {
            String subLayer = "";
            for (int i = 0; i < trim; i++) {
                subLayer = subLayer.concat(".");
            }
            for (int i = 0; i < layers.size(); i++) {
                final String newLayer = layers.get(i).substring(0, 16 - trim) + subLayer;
                rhythm.addOneTimeAltLayer(i, rhythm.getLength() - 1, newLayer);
            }
        }
    }


}
