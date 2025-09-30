package com.wallet.transaction.model;



public enum TxnStatusEnum {
    PENDING, SUCCESS, FAILED;


    /**
         * A terminal state is one that should not be changed anymore.
         */
        public boolean isTerminal() {
            return this == SUCCESS || this == FAILED;
        }
    }

