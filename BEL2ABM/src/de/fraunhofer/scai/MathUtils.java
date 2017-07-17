package de.fraunhofer.scai;

public class MathUtils {

	// returns the cumulative normal distribution function (CNDF)
	// for a standard normal: N(0,1)
	//source: http://stackoverflow.com/questions/442758/which-java-library-computes-the-cumulative-standard-normal-distribution-function
	//        http://www.codeproject.com/Messages/2622967/Re-NORMSDIST-function.aspx
	static double CNDF(double x)
	{
	    int neg = (x < 0d) ? 1 : 0;
	    if ( neg == 1) 
	        x *= -1d;

	    double k = (1d / ( 1d + 0.2316419 * x));
	    double y = (((( 1.330274429 * k - 1.821255978) * k + 1.781477937) *
	                   k - 0.356563782) * k + 0.319381530) * k;
	    y = 1.0 - 0.398942280401 * Math.exp(-0.5 * x * x) * y;

	    return (1d - neg) * y + neg * (1d - y);
	}
}
