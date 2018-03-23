/*
forum discussion for detailed explanation
https://www.coursera.org/learn/cryptocurrency/programming/KOo3V/scrooge-coin/discussions/threads/Yu1-ncdjEeaQDgq_dR_6Tg/replies/-DEdGseoEeaQDgq_dR_6Tg
*/

import java.util.*;
import java.security.PublicKey;

public class TxHandler {

    public UTXOPool ledger;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        ledger = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS
        HashSet<UTXO> UTXOsHashSet = new HashSet<UTXO>();
        double ownedCoinSum = 0;
        double spentCoinSum = 0;

        for (int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input input = tx.getInput(i);

            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);

            Transaction.Output output = ledger.getTxOutput(utxo);

            if (!ledger.contains(utxo)) // condition #1
                return false;

            PublicKey pubKey = output.address;
            byte[] message = tx.getRawDataToSign(i);
            byte[] signature = input.signature;
            if (!Crypto.verifySignature(pubKey, message, signature)) // condition #2
                return false; 

            if (UTXOsHashSet.contains(utxo)) // condition #3
                return false;
            UTXOsHashSet.add(utxo);

            ownedCoinSum += output.value;
        }

        for (Transaction.Output output : tx.getOutputs()) {
            if (output.value < 0) // condition #4
                return false;

            spentCoinSum += output.value;
        }

        return ownedCoinSum >= spentCoinSum; // condition #5
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        HashSet<Transaction> validTxs = new HashSet<Transaction>();

        for (Transaction tx : possibleTxs) {
            if (isValidTx(tx)) {
                validTxs.add(tx);

                for (Transaction.Input input : tx.getInputs()) {
                    UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                    ledger.removeUTXO(utxo);
                }

                for (int i = 0; i < tx.numOutputs(); i++) {
                    Transaction.Output output = tx.getOutput(i);
                    UTXO utxo = new UTXO(tx.getHash(), i);
                    ledger.addUTXO(utxo, output);
                }
            }   
        }
        
        Transaction[] res = new Transaction[validTxs.size()];
        res = validTxs.toArray(res);
        return res;
    }

    public UTXOPool getUTXOPool() {
        return ledger;
    }
}