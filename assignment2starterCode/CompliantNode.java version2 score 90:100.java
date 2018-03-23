import java.util.*;
import static java.util.stream.Collectors.toSet;

/* CompliantNode refers to a node that follows the rules (not malicious)*/
public class CompliantNode implements Node {

    private double p_graph;
    private double p_malicious;
    private double p_txDistribution;
    private int numRounds;
    private int currRound;
    private int trueFolloweesLength;
    private int trueBlackListLength;
    private boolean[] followees;
    private boolean[] blackList;
    private Set<Transaction> pendingTransactions;
    private Map<Transaction, Set<Integer>> uniqueTxs = new HashMap<Transaction, Set<Integer>>();

    public CompliantNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
        // IMPLEMENT THIS
        this.p_graph = p_graph;
        this.p_malicious = p_malicious;
        this.p_txDistribution = p_txDistribution;
        this.numRounds = numRounds;
        currRound = 0;
        trueFolloweesLength = 0;
        trueBlackListLength = 0;
    }

    /** {@code followees[i]} is true if and only if this node follows node {@code i} */
    public void setFollowees(boolean[] followees) {
        // IMPLEMENT THIS
    	this.followees = followees;
        this.blackList = new boolean[followees.length];
        for (int i = 0; i < followees.length; i++) 
            if (followees[i])
                trueFolloweesLength++;
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        // IMPLEMENT THIS
        this.pendingTransactions = pendingTransactions;
    }

    public Set<Transaction> sendToFollowers() {
        // IMPLEMENT THIS
        Set<Transaction> consensusTxs = new HashSet<Transaction>(pendingTransactions);
        // clear the pendingTransactions for the next round
        pendingTransactions.clear();

        if (currRound == numRounds) {
            Set<Transaction> Txs = new HashSet<Transaction>();
            for (Transaction tx : consensusTxs) {
                int count = uniqueTxs.get(tx).size();
                double size = Math.min(trueFolloweesLength- trueBlackListLength, 
                                       (trueFolloweesLength- trueBlackListLength)* p_graph * p_txDistribution * numRounds * p_malicious);
                if (count >= size)
                    Txs.add(tx);
            }
            return Txs;
        }

        return consensusTxs;
    }

    public void receiveFromFollowees(Set<Candidate> candidates) {
        // IMPLEMENT THIS
        currRound++;
    	Set<Integer> senders = candidates.stream().map(c -> c.sender).collect(toSet());
        // if there is no transaction sent from a node
        // then the node should be a malicious node
    	for (int i = 0; i < followees.length; i++) {
    		if (followees[i] && !senders.contains(i))
    			blackList[i] = true;
    	}

        // only receive transaction from nodes that are compliant
    	for (Candidate c: candidates) {
    		if (!blackList[c.sender]) {
    			pendingTransactions.add(c.tx);

                if (!uniqueTxs.containsKey(c.tx)) {
                    Set<Integer> s = new HashSet<Integer>();
                    uniqueTxs.put(c.tx, s);
                }
                 // update the unique sender list for a transaction
                uniqueTxs.get(c.tx).add(c.sender);
            }
    	}

        for (int i = 0; i < blackList.length; i++) 
            if (blackList[i])
                trueBlackListLength++;
    }
}
