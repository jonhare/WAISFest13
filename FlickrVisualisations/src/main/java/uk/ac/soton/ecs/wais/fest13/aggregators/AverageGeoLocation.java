/**
 * 
 */
package uk.ac.soton.ecs.wais.fest13.aggregators;

import java.util.Collection;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocation;
import uk.ac.soton.ecs.wais.fest13.Aggregator;
import uk.ac.soton.ecs.wais.fest13.SocialComment;
import uk.ac.soton.ecs.wais.fest13.UserInformation;

/**
 *	Calculates the average geolocation.	
 *
 *	@author David Dupplaw (dpd@ecs.soton.ac.uk)
 *  @created 12 Sep 2013
 *	@version $Author$, $Revision$, $Date$
 */
public class AverageGeoLocation implements Aggregator<GeoLocation>
{
	@Override
	public GeoLocation aggregate( Collection<SocialComment> comments, UserInformation ui )
	{
		double lng = 0;
		double lat = 0;
		
		for( SocialComment sc : comments )
		{
			lng += sc.location.longitude;
			lat += sc.location.latitude;
		}
		
		return new GeoLocation( lat/comments.size(), lng/comments.size() );
	}
}
