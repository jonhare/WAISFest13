/**
 * 
 */
package uk.ac.soton.ecs.wais.fest13.sound;

import uk.ac.soton.ecs.wais.fest13.SocialComment;
import uk.ac.soton.ecs.wais.fest13.UserInformation;

/**
 *	Takes a social comment and plays an appropriate sound.
 *
 *	@author David Dupplaw (dpd@ecs.soton.ac.uk)
 *  @created 12 Sep 2013
 */
public interface SoundTranslator
{
	/**
	 * 	For the given social comment and the given information about the user,
	 * 	this sound translator should generate an appropriate sound.
	 * 
	 *	@param comment The social comment.
	 *	@param userInformation The user information.
	 */
	public void translate( SocialComment comment, UserInformation userInformation );
	
	/**
	 *  Stop all ongoing translations.
	 */
	public void stop();
}
