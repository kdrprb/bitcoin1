import java.util.ArrayList;
import java.util.HashSet;

public class TxHandler {
    
    private UTXOPool myPool; 

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        myPool = new UTXOPool(utxoPool);
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
        boolean isValidTx = true;
        HashSet<UTXO> claimedUTXOs = new HashSet<UTXO>();
        double allInputs=0;
        double allOutputs=0;
        
        VALIDATE: for(int n=0; n<tx.numInputs(); n++)
        {
            Transaction.Input currentInput = tx.getInput(n);
            UTXO currentUTXO = new UTXO(currentInput.prevTxHash,currentInput.outputIndex);
            Transaction.Output previousOutput = myPool.getTxOutput(currentUTXO);

            // requirement #1
            if(previousOutput==null)
            {
                isValidTx=false;
                break VALIDATE;
            }
            
            // requirement #2
            boolean validSignature = Crypto.verifySignature(previousOutput.address, tx.getRawDataToSign(n), currentInput.signature);
            if(!validSignature)
            {
                isValidTx=false;
                break VALIDATE;
            }
            
            // requirement #3
            if(claimedUTXOs.contains(currentUTXO))
            {
                isValidTx=false;
                break VALIDATE;
            }
            else
            {
                claimedUTXOs.add(currentUTXO);
            }
            
            // requirement #5
            allInputs+=previousOutput.value;
        }// end VALIDATE: for(int n=0; n<tx.numInputs(); n++)
        
        VALIDATE2: for(int n=0; n<tx.numOutputs(); n++)
        {
            // requirement #4
            Transaction.Output currentOutput = tx.getOutput(n);
            if(currentOutput.value<0)
            {
                isValidTx=false;
                break VALIDATE2;
            }
            
            // requirement #5
            allOutputs+=currentOutput.value;
        }// end VALIDATE2: for(int n=0; n<tx.numOutputs(); n++)

        // requirement #5
        if(allInputs<allOutputs)
        {
            isValidTx=false;
        }
        
        return isValidTx;
    }

    private void handleOneTx(Transaction t)
    {
        for(int n=0; n<t.numInputs(); n++)
        {            
            Transaction.Input currentInput = t.getInput(n);
            Transaction.Output currentOutput = t.getOutput(n);
            UTXO currentUTXO = new UTXO(currentInput.prevTxHash,currentInput.outputIndex);
            myPool.removeUTXO(currentUTXO);
            UTXO newUTXO = new UTXO(t.getHash(),n);
            myPool.addUTXO(newUTXO,currentOutput);
        }
        
    }
    
    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        ArrayList<Transaction> validTxList = new ArrayList<Transaction>();
        
        for(int n=0; n<possibleTxs.length; n++)
        {
            Transaction currentTransaction = possibleTxs[n];
            if(isValidTx(currentTransaction))
            {
                handleOneTx(currentTransaction);
            }
        }
        
        return validTxList.toArray(new Transaction[validTxList.size()]);
    }

}
