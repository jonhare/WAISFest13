/**
 * 
 */
package uk.ac.soton.ecs.wais.fest13.sound;

import java.util.Collection;

import uk.ac.soton.ecs.wais.fest13.SocialComment;
import uk.ac.soton.ecs.wais.fest13.UserInformation;

/**
 *	Takes a set of social comments and plays an appropriate sound.
 *
 *	@author David Dupplaw (dpd@ecs.soton.ac.uk)
 *  @created 12 Sep 2013
 */
public interface SoundTranslator
{
	/**
	 * 	For the given set of social comments and the given information about the user,
	 * 	this sound translator should generate an appropriate sound.
	 * 
	 *	@param comment The aggregation of social comments.
	 *	@param userInformation The user information.
	 */
	public void translate( Collection<SocialComment> comment, UserInformation userInformation );
	
	/**
	 *  Stop all ongoing translations.
	 */
	public void stop();
}
