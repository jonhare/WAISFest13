/**
 * 
 */
package uk.ac.soton.ecs.wais.fest13;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocation;

/**
 *	Information about a social comment.	
 *
 *	@author David Dupplaw (dpd@ecs.soton.ac.uk)
 *  @created 12 Sep 2013
 */
public class SocialComment
{
	/** The comment that was made */
	public String theComment;
	
	/** The location from which the comment was made */
	public GeoLocation location;
	
	/** The calculated sentiment score for the comment */
	public double sentimentScore = 0.5;
}
