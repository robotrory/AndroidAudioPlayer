package com.smithyproductions.audioplayer;

/**
 * Created by rory on 07/01/16.
 */
public class PlayerStateMachine {

    enum PlayerState {UNINITIALISED, ERROR}

    private PlayerState currentState;

    private PlayerState[][] validTransitions = {
            {PlayerState.UNINITIALISED}
    };



    public void tryToMoveState(final PlayerState state) {

    }
}
