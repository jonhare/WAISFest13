/**
 * 
 */
package uk.ac.soton.ecs.wais.fest13.aggregators;

import java.util.Collection;

import uk.ac.soton.ecs.wais.fest13.Aggregator;
import uk.ac.soton.ecs.wais.fest13.SocialComment;
import uk.ac.soton.ecs.wais.fest13.UserInformation;

/**
 *	Calculated average sentiment for a set of social comments.
 *
 *	@author David Dupplaw (dpd@ecs.soton.ac.uk)
 *  @created 13 Sep 2013
 *	@version $Author$, $Revision$, $Date$
 */
public class AverageSentimentAggregator implements Aggregator<Double>
{
	@Override
	public Double aggregate( Collection<SocialComment> comments, UserInformation ui )
	{
		double acc = 0;
		for( SocialComment sc : comments )
			acc += sc.sentimentScore;
		return acc/(double)comments.size();
	}
}
