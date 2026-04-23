package com.petlife.server.modules.pet.persistence.command;

/**
 * 宠物归档命令。
 */
public class ArchivePetCommand {

    private Long petId;
    private String archiveStatus;

    public Long getPetId() {
        return petId;
    }

    public void setPetId(Long petId) {
        this.petId = petId;
    }

    public String getArchiveStatus() {
        return archiveStatus;
    }

    public void setArchiveStatus(String archiveStatus) {
        this.archiveStatus = archiveStatus;
    }
}
