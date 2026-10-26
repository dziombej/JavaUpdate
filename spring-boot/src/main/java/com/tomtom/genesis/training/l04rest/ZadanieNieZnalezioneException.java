package com.tomtom.genesis.training.l04rest;

public class ZadanieNieZnalezioneException extends RuntimeException {

    private final long id;

    public ZadanieNieZnalezioneException(long id) {
        super("Nie ma zadania o id " + id);
        this.id = id;
    }

    public long id() {
        return id;
    }
}
