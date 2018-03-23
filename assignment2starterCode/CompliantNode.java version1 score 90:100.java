// https://github.com/msilb/coursera-cryptocurrency/blob/master/assignment-2-consensus-from-trust/CompliantNode.java

import java.util.*;
import static java.util.stream.Collectors.toSet;

/* CompliantNode refers to a node that follows the rules (not malicious)*/
public class CompliantNode implements Node {

    private double p_graph;
    private double p_malicious;
    private double p_txDistribution;
    private double numRounds;
    private boolean[] followees;
    private boolean[] blackList;
    private Set<Transaction> pendingTransactions;

    public CompliantNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
        // IMPLEMENT THIS
        this.p_graph = p_graph;
        this.p_malicious = p_malicious;
        this.p_txDistribution = p_txDistribution;
        this.numRounds = numRounds;
    }

    /** {@code followees[i]} is true if and only if this node follows node {@code i} */
    public void setFollowees(boolean[] followees) {
        // IMPLEMENT THIS
    	this.followees = followees;

    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        // IMPLEMENT THIS
        this.pendingTransactions = pendingTransactions;
        this.blackList = new boolean[followees.length];
    }

    public Set<Transaction> sendToFollowers() {
        // IMPLEMENT THIS
        Set<Transaction> consensusTxs = new HashSet<Transaction>(pendingTransactions);
        pendingTransactions.clear();
        return consensusTxs;
    }

    public void receiveFromFollowees(Set<Candidate> candidates) {
        // IMPLEMENT THIS
    	Set<Integer> senders = candidates.stream().map(c -> c.sender).collect(toSet());
    	for (int i = 0; i < followees.length; i++) {
    		if (followees[i] && !senders.contains(i))
    			blackList[i] = true;
    	}

    	for (Candidate c: candidates) {
    		if (!blackList[c.sender])
    			pendingTransactions.add(c.tx);
    	}
    }
}
