package com.rudra.myassistant.model;

public class SuggestionCardModel {
    private String cardType;
    private String iconResName;
    private String tagLabel;
    private String title;
    private String subtitle;
    private String description;
    private String primaryButtonLabel;
    private String actionKey;
    private String actionPayload;

    public SuggestionCardModel() {}

    public SuggestionCardModel(String cardType, String iconResName, String title, String primaryButtonLabel) {
        this.cardType = cardType;
        this.iconResName = iconResName;
        this.title = title;
        this.primaryButtonLabel = primaryButtonLabel;
    }

    public String getCardType() { return cardType; }
    public void setCardType(String cardType) { this.cardType = cardType; }

    public String getIconResName() { return iconResName; }
    public void setIconResName(String iconResName) { this.iconResName = iconResName; }

    public String getTagLabel() { return tagLabel; }
    public void setTagLabel(String tagLabel) { this.tagLabel = tagLabel; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPrimaryButtonLabel() { return primaryButtonLabel; }
    public void setPrimaryButtonLabel(String primaryButtonLabel) { this.primaryButtonLabel = primaryButtonLabel; }

    public String getActionKey() { return actionKey; }
    public void setActionKey(String actionKey) { this.actionKey = actionKey; }

    public String getActionPayload() { return actionPayload; }
    public void setActionPayload(String actionPayload) { this.actionPayload = actionPayload; }
}
