package semantics.tolerancepair;

/**
 * Progress listener for a tolerance-pair creation. Implementing objects can be
 * registered to receive progress updates.
 * 
 * @author Tobias Falke
 * 
 */
public interface ProgressListener {

	/**
	 * Method called by a tolerance-pair creator to provide the current
	 * progress.
	 * @param progress
	 *        Current progress, between 0 and 1
	 * @return Continue flag, if set to false, the creator aborts its search
	 *         process
	 */
	public boolean progressChanged(double progress);

}
