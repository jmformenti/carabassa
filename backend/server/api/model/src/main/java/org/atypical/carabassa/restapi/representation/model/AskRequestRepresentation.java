package org.atypical.carabassa.restapi.representation.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AskRequestRepresentation {

    @NotBlank
    @Size(max = 1000)
    private String question;

    public AskRequestRepresentation() {
        super();
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }
}
