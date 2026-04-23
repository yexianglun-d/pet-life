package com.petlife.server.modules.pet.persistence.command;

/**
 * 宠物删除命令。
 */
public class DeletePetCommand {

    private Long petId;

    public Long getPetId() {
        return petId;
    }

    public void setPetId(Long petId) {
        this.petId = petId;
    }
}
