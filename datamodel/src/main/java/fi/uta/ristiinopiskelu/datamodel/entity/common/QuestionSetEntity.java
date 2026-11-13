package fi.uta.ristiinopiskelu.datamodel.entity.common;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.QuestionSet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuestionSetEntity {

    private LocalisedStringEntity title;
    private List<LocalisedStringEntity> questions = new ArrayList<LocalisedStringEntity>();

    public static QuestionSetEntity from(QuestionSet source) {
        if (source == null) {
            return null;
        }
        QuestionSetEntity target = new QuestionSetEntity();
        target.setTitle(LocalisedStringEntity.from(source.getTitle()));
        target.setQuestions(source.getQuestions() == null ? null
            : source.getQuestions().stream().map(LocalisedStringEntity::from).toList());
        return target;
    }

    public static List<QuestionSetEntity> from(List<QuestionSet> source) {
        if (source == null) {
            return null;
        }
        return source.stream().map(QuestionSetEntity::from).toList();
    }
}
