package fi.uta.ristiinopiskelu.datamodel.dto.v8;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * When registering questions are asked from the student. Layout differs from choice, this produces a text area presentation.
 * 
 * fi: ilmoittautumisen yhteydessä kysyttävät kysymykset
 * 
 * @author Eero Manninen <eero.manninen@studyo.fi> 
 *         Based on https://wiki.eduuni.fi/download/attachments/70202805/CSC6_curriculum_api_1.0.0-oas3_swagger.json?version=1&modificationDate=1531828139937&api=v2
 */
public class QuestionSet {

    @JsonProperty("title")
    private LocalisedString title;

    @JsonProperty("questions")
    private List<LocalisedString> questions = new ArrayList<LocalisedString>();

    /**
     * @return LocalisedString return the title
     */
    public LocalisedString getTitle() {
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(LocalisedString title) {
        this.title = title;
    }

    /**
     * @return List<LocalisedString> return the questions
     */
    public List<LocalisedString> getQuestions() {
        return questions;
    }

    /**
     * @param questions the questions to set
     */
    public void setQuestions(List<LocalisedString> questions) {
        this.questions = questions;
    }

}
