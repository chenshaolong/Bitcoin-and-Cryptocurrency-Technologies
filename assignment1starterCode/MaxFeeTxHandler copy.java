/*
forum discussion for detailed explanation
https://www.coursera.org/learn/cryptocurrency/programming/KOo3V/scrooge-coin/discussions/threads/Yu1-ncdjEeaQDgq_dR_6Tg/replies/-DEdGseoEeaQDgq_dR_6Tg

more detailed explanation from
https://freedom-to-tinker.com/2014/10/27/bitcoin-mining-is-np-hard/
*/

import java.util.*;
import java.security.PublicKey;

public class MaxFeeTxHandler {

    public static final int VALID = 1;
    public static final int INVALID = -1;
    public static final int PARTIAL_VALID = 0;    

    public UTXOPool ledger;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public MaxFeeTxHandler(UTXOPool utxoPool) {
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


    /*
     * Similar to above, but returns either VALID, PARTIAL_VALID (if not 
     *  all inputs are in UTXO pool but everything else checks), or INVALID. 
     */
    public int classifyTx(Transaction tx) {
        HashSet<UTXO> UTXOsHashSet = new HashSet<UTXO>();
        double ownedCoinSum = 0;
        double spentCoinSum = 0;

        for (int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input input = tx.getInput(i);

            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);

            Transaction.Output output = ledger.getTxOutput(utxo);

            //if the transaction pool doesn't contain it yet
            if (!ledger.contains(utxo)) // condition #1
                return PARTIAL_VALID;

            PublicKey pubKey = output.address;
            byte[] message = tx.getRawDataToSign(i);
            byte[] signature = input.signature;
            if (!Crypto.verifySignature(pubKey, message, signature)) // condition #2
                return INVALID; 

            if (UTXOsHashSet.contains(utxo)) // condition #3
                return INVALID;
            UTXOsHashSet.add(utxo);

            ownedCoinSum += output.value;
        }

        for (Transaction.Output output : tx.getOutputs()) {
            if (output.value < 0) // condition #4
                return INVALID;

            spentCoinSum += output.value;
        }

        if (ownedCoinSum < spentCoinSum) // condition #5
            return INVALID;

        return VALID;
    }

    // classify transactions and createa wrapper
    public TxWrapper wrapTx(Transaction tx) {
        int res = VALID;

        HashSet<UTXO> UTXOsHashSet = new HashSet<UTXO>();
        double ownedCoinSum = 0;
        double spentCoinSum = 0;

        for (int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input input = tx.getInput(i);

            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);

            Transaction.Output output = ledger.getTxOutput(utxo);

            //if the transaction pool doesn't contain it yet
            if (!ledger.contains(utxo)) { // condition #1
                res = PARTIAL_VALID;
                ownedCoinSum = -1;
            } else {
                ownedCoinSum += output.value;
            }

            PublicKey pubKey = output.address;
            byte[] message = tx.getRawDataToSign(i);
            byte[] signature = input.signature;
            if (!Crypto.verifySignature(pubKey, message, signature)) // condition #2
                return null; 

            if (UTXOsHashSet.contains(utxo)) // condition #3
                return null;
            UTXOsHashSet.add(utxo);
        }

        for (Transaction.Output output : tx.getOutputs()) {
            if (output.value < 0) // condition #4
                return null;

            spentCoinSum += output.value;
        }

        if (ownedCoinSum != -1 && ownedCoinSum < spentCoinSum) // condition #5
            return null;

        return new TxWrapper(new Transaction(tx), ownedCoinSum - spentCoinSum, res);
    }

    //this only checks if all the inputs are in the UTXO pool
    public int quickCheck(TxWrapper wrappedTx) {
        Transaction tx = wrappedTx.getTx();
        double ownedCoinSum = 0;

        for (int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input input = tx.getInput(i);

            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);

            //if the transaction pool doesn't contain it yet
            if (!ledger.contains(utxo))  // condition #1
                return PARTIAL_VALID;

            Transaction.Output output = ledger.getTxOutput(utxo);

            PublicKey pubKey = output.address;
            byte[] message = tx.getRawDataToSign(i);
            byte[] signature = input.signature;
            if (!Crypto.verifySignature(pubKey, message, signature)) // condition #2
                return INVALID; 

            ownedCoinSum += output.value;
        }

        wrappedTx.setFee(wrappedTx.getFee() - ownedCoinSum);
        return VALID;
    }

    // total transaction fees = sum of input values - sum of output values
    private double totalTxFees(Transaction tx) {
        double ownedCoinSum = 0; // sum of input values
        double spentCoinSum = 0; // sum of output values

        for (Transaction.Input input : tx.getInputs()) {
            if (!isValidTx(tx)) continue;

            UTXO uxto = new UTXO(input.prevTxHash, input.outputIndex);

            Transaction.Output output = ledger.getTxOutput(uxto);

            ownedCoinSum += output.value;
        }

        for (Transaction.Output output : tx.getOutputs()) {
            if (!isValidTx(tx)) continue;

            spentCoinSum += output.value;
        }

        return ownedCoinSum - spentCoinSum;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        return greedyHandleTxs(possibleTxs);
    }

    public Transaction[] basicHandleTxs(Transaction[] possibleTxs) {
        HashSet<Transaction> validTxs = new HashSet<Transaction>();

        boolean isDone = false;

        while (!isDone) {
            isDone = true;

            for (Transaction tx : possibleTxs) {
                if (tx == null) continue;
                if (isValidTx(tx)) {
                    validTxs.add(tx);

                    // Remove old UTXOs from Pool
                    for (Transaction.Input input : tx.getInputs()) {
                        UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                        ledger.removeUTXO(utxo);
                    }

                    // Add new UTXOs to Pool
                    for (int i = 0; i < tx.numOutputs(); i++) {
                        Transaction.Output output = tx.getOutput(i);
                        UTXO utxo = new UTXO(tx.getHash(), i);
                        ledger.addUTXO(utxo, output);
                    }

                    // Set Array Element to Null
                    tx = null;

                    // not Done yet
                    isDone = false;
                }
            }
        }
        
        Transaction[] res = new Transaction[validTxs.size()];
        res = validTxs.toArray(res);
        return res;
    }

    // node for graph
    public class TxWrapper implements Comparable<TxWrapper> {
        private Transaction tx;
        private double fee;
        private int validity;
        private ArrayList<TxWrapper> refs;

        public TxWrapper(Transaction tx_, double fee_, int validity_) {
            tx = tx_;
            fee =fee_;
            validity = validity_;
            refs = new ArrayList<TxWrapper>();
        }

        public Transaction getTx() {
            return tx;
        }

        public void setTx(Transaction tx_) {
            tx = tx_;
        }

        public double getFee() {
            return fee;
        }

        public void setFee(double fee_) {
            fee =fee_;
        }

        public int getValidity() {
            return validity;
        }

        public void setValidity(int validity_) {
            validity = validity_;
        }

        public ArrayList<TxWrapper> getRefs() {
            return refs;
        }

        public void addRefs(TxWrapper txw) {
            refs.add(txw);
        }

        public int compareTo(TxWrapper txw) {
            return Double.compare(fee, txw.getFee());
        }
    }


    //Plan
    // (1) first create a hash to transaction table for possibleTxs
    /* (2) for each transaction, if it's invalid, then kill it. 
     *  If all inputs are in UTXOPool, add it to nbrsOfGood. 
     *  For each input, add that transaction to the "refs" list 
     *  of the referenced address.
     * (3) order potGoodTxs by transaction fee (make txFee a method).
     *  Repeat until potGoodTxs is empty: take the transaction tx with maximum fee in 
     *  nbrsOfGood and if
     *  it's valid, then put it in UTXOPool.
     *  Take any transactions that attempt to double-spend the addresses just spent
     *  and delete them from potGoodTxs (optional).
     *   Check neighbors of tx; if they are valid put them into nbrsOfGood.
     */
    public Transaction[] greedyHandleTxs(Transaction[] possibleTxs) {
        HashMap<byte[], TxWrapper> hashToTx = new HashMap<byte[], TxWrapper>();
        PriorityQueue<TxWrapper> nbrsOfGood = new PriorityQueue<TxWrapper>();
        ArrayList<TxWrapper> partialGoodTxs = new ArrayList<TxWrapper>();
        ArrayList<Transaction> goodTxs = new ArrayList<Transaction>();

        for (Transaction tx : possibleTxs) {
            TxWrapper txw = wrapTx(tx);
            if (txw == null) continue; // don't put in the hashmap
            hashToTx.put(tx.getHash(), txw);

            switch (txw.getValidity()) {
                case VALID:
                    nbrsOfGood.add(txw);
                    break;
                case PARTIAL_VALID:
                    partialGoodTxs.add(txw);
                    break;
                // case INVALID:
                // do nothing
            }
        }

        for (TxWrapper txw : partialGoodTxs) {
            for (Transaction.Input input : txw.getTx().getInputs()) {
                byte[] hash = input.prevTxHash;
                TxWrapper origin = hashToTx.get(hash);
                UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);

                if (origin == null && !ledger.contains(utxo))
                    break;

                origin.addRefs(txw);
            }
        }

        while (!nbrsOfGood.isEmpty()) {
            TxWrapper top = nbrsOfGood.poll();
            if (quickCheck(top) != VALID) continue;

            goodTxs.add(top.getTx());

            for (Transaction.Input input : top.getTx().getInputs()) {
                UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                ledger.removeUTXO(utxo);
            }

            for (int i = 0; i < top.getTx().numOutputs(); i++) {
                Transaction.Output output = top.getTx().getOutput(i);
                UTXO utxo = new UTXO(top.getTx().getHash(), i);
                ledger.addUTXO(utxo, output);
            }

            for (TxWrapper nbr : top.getRefs()) {
                if (quickCheck(nbr) == VALID)
                    nbrsOfGood.add(nbr);
            }
        }

        Transaction[] res = new Transaction[goodTxs.size()];
        res = goodTxs.toArray(res);
        return res;
    }

    public class TxHandlerState implements Comparable<TxHandlerState> {
        public TxHandler handler;
        public PriorityQueue<TxWrapper> nbrsOfGood;
        public ArrayList<TxWrapper> partialGoodTxs;
        public ArrayList<Transaction> goodTxs;
        public double fees;
        public TxHandlerState(TxHandler th) {
            handler = th;
            nbrsOfGood = new PriorityQueue<TxWrapper>();
            partialGoodTxs = new ArrayList<TxWrapper>();
            goodTxs = new ArrayList<Transaction>();
            fees = 0;
        }

        public int compareTo(TxHandlerState t) {
            return Double.compare(fees, t.fees);
        }
    }

    public class TxSearch extends HeuristicSearch<TxHandlerState> {
        public TxSearch(TxHandlerState e) {
            super(e);
        }

        boolean test(TxHandlerState e) {
            return e.nbrsOfGood.isEmpty();
        }

        ArrayList<TxHandlerState> children(TxHandlerState e) {
            return null;
        }
    }

    public abstract class HeuristicSearch<E extends Comparable<E>> {
        abstract boolean test(E e);
        abstract ArrayList<E> children(E e);
        protected PriorityQueue<E> options;

        public HeuristicSearch(E e) {
            options = new PriorityQueue<E>();
            options.add(e);
        }

        //Collections.reverseOrder
        public HeuristicSearch(E e, Comparator<E> c) {
            options = new PriorityQueue<E>(11, c);
            options.add(e);
        }

        //trying to minimize the heuristic
        public E heuristicMinDFS() {
            while (!options.isEmpty()) {
                E top = options.poll();
                if (test(top)) {
                    return top;
                }
                options.addAll(children(top));
            }
            return null; // solution not found
        }

        //trying to maximize the heuristic
        //DO: add a time!
        public E heuristicMaxDFS() {
            E best = null;
            while (!options.isEmpty()) {
                E top = options.poll();
                if (test(top)) {
                    if (best == null || top.compareTo(best) == 1)
                        best = top;
                } else {
                    options.addAll(children(top));
                }
            }
            return best; // solution not found
        }
    }

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
    }
}