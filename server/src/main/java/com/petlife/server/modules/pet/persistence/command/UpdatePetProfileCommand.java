package com.petlife.server.modules.pet.persistence.command;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 宠物主档更新命令。
 */
public class UpdatePetProfileCommand {

    private Long petId;
    private Long userId;
    private String petName;
    private String petType;
    private String breed;
    private String gender;
    private LocalDate birthday;
    private LocalDate adoptDate;
    private Integer neuterStatus;
    private String avatarUrl;
    private BigDecimal weightKg;
    private String allergyNotes;
    private String medicalHistory;

    public Long getPetId() {
        return petId;
    }

    public void setPetId(Long petId) {
        this.petId = petId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getPetName() {
        return petName;
    }

    public void setPetName(String petName) {
        this.petName = petName;
    }

    public String getPetType() {
        return petType;
    }

    public void setPetType(String petType) {
        this.petType = petType;
    }

    public String getBreed() {
        return breed;
    }

    public void setBreed(String breed) {
        this.breed = breed;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    public LocalDate getAdoptDate() {
        return adoptDate;
    }

    public void setAdoptDate(LocalDate adoptDate) {
        this.adoptDate = adoptDate;
    }

    public Integer getNeuterStatus() {
        return neuterStatus;
    }

    public void setNeuterStatus(Integer neuterStatus) {
        this.neuterStatus = neuterStatus;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(BigDecimal weightKg) {
        this.weightKg = weightKg;
    }

    public String getAllergyNotes() {
        return allergyNotes;
    }

    public void setAllergyNotes(String allergyNotes) {
        this.allergyNotes = allergyNotes;
    }

    public String getMedicalHistory() {
        return medicalHistory;
    }

    public void setMedicalHistory(String medicalHistory) {
        this.medicalHistory = medicalHistory;
    }
}
