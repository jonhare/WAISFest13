/**
 * 
 */
package uk.ac.soton.ecs.wais.fest13;

import java.util.Collection;

/**
 *	
 *
 *	@author David Dupplaw (dpd@ecs.soton.ac.uk)
 *  @created 12 Sep 2013
 *	@version $Author$, $Revision$, $Date$
 */
public interface Aggregator<T>
{
	/**
	 * 	Given a collection of social comments and the user's information,
	 * 	returns the typed object.
	 * 
	 *	@param comments The collections of comments
	 *	@param ui The user information
	 *	@return The return type
	 */
	public T aggregate( Collection<SocialComment> comments, UserInformation ui );
}
