package selection;

import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Line;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.process.ImageProcessor;

import java.awt.Button;
import java.awt.Color;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.List;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import mpicbg.imglib.util.Util;
import mpicbg.models.Point;
import fiji.tool.AbstractTrackingTool;
import fiji.tool.ToolToggleListener;
import fiji.tool.ToolWithOptions;
import fiji.util.gui.GenericDialogPlus;

/**
 * This is a template for a generic tool using Fiji's AbstractTool infrastructure.
 */

public class Select_Points extends AbstractTrackingTool implements ToolToggleListener, ToolWithOptions 
{
	{
		// for debugging, all custom tools can be removed to make space for this one if necessary
		clearToolsIfNecessary = true;
	}

	Color standard = Color.GREEN;
	
	/**
	 * which windows to select in
	 */
	final ImagePlus imp1, imp2;
	
	/**
	 * the lists of points
	 */
	final List list1 = new List( 20 );
    final List list2 = new List( 20 );

	/**
	 * what is currently clicked
	 */
	Point p1 = null;
	Point p2 = null;
	float radius1 = 0;
	float radius2 = 0;
	
	/**
	 * The matches 
	 */
	final ArrayList< ExtendedPointMatch > matches = new ArrayList< ExtendedPointMatch >();
	
	public Select_Points( final ImagePlus imp1, final ImagePlus imp2 )
	{
		this.imp1 = imp1;
		this.imp2 = imp2;

		initDisplay();
	}	

	public void initDisplay()
	{
		final Frame frame = new Frame( "Selected Landmarks" );
		frame.setSize( 1000, 600 );        
		
		/* Instantiation */		
		final GridBagLayout layout = new GridBagLayout();
		final GridBagConstraints c = new GridBagConstraints();
		
		/* Elements */
		final String title1 = imp1.getTitle().substring( 0 , Math.min( 30,  imp1.getTitle().length() ) );
		final String title2 = imp2.getTitle().substring( 0 , Math.min( 30,  imp2.getTitle().length() ) );
		
		final Button button = new Button( "Done" );
		final Button remove = new Button( "Remove" );
	    final Button save = new Button( "Save" );
	    final Button load = new Button( "Load" );

	    final Label text1 = new Label( title1, Label.CENTER );
	    final Label text2 = new Label( title2, Label.CENTER );

	    /* Location */
	    frame.setLayout( layout );

	    c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
	    frame.add ( text1, c );
	    
	    c.gridx++;
	    frame.add ( text2, c );
	    c.gridx--;
	    
	    ++c.gridy;
	    c.insets = new Insets( 10, 0, 0, 0 );
	    frame.add( list1, c );	
	    ++c.gridx;
	    frame.add( list2, c );
	    c.gridx--;

	    ++c.gridy;
	    c.insets = new Insets( 10, 0, 0, 0 );
	    frame.add( remove, c );
	    ++c.gridy;
	    frame.add( load, c );
	    ++c.gridx;
	    frame.add( save, c );
	    --c.gridx;
	    ++c.gridy;
	    c.insets = new Insets( 20, 0, 0, 0 );
	    frame.add( button, c );
	    
	    /* Configuration */
	    button.addActionListener( new DoneButtonListener( frame ) );
	    remove.addActionListener( new RemoveItemListener( list1, list2, matches ) );
	    save.addActionListener( new SaveListener( frame, matches ) );
	    load.addActionListener( new LoadListener( frame, list1, list2, matches ) );
	    list1.addItemListener( new ListListener( list1, list2 ) );
	    list2.addItemListener( new ListListener( list2, list1 ) );
	    
		frame.setVisible( true );
	}
	
	public void drawCurrentSelection( final Color color )
	{
		if ( matches.size() == 0 )
			return;
		
		final Overlay o1 = new Overlay();
		final Overlay o2 = new Overlay();
		
		final int currentSlice1 = imp1.getCurrentSlice();
		final int currentSlice2 = imp2.getCurrentSlice();
		
		for ( final ExtendedPointMatch pm : matches )
		{
			final Point p1 = pm.getP1();
			final Point p2 = pm.getP2();
			
			final float radius1 = pm.radius1L;
			final float radius2 = pm.radius2L;
			
			// assuming z has a different calibration
			final float distance1 = Math.abs( p1.getL()[ 2 ] - currentSlice1 ) * ( (float)imp1.getCalibration().getZ( 1 ) / (float)imp1.getCalibration().getX( 1 ) ); 
			final float distance2 = Math.abs( p2.getL()[ 2 ] - currentSlice2 ) * ( (float)imp2.getCalibration().getZ( 1 ) / (float)imp2.getCalibration().getX( 1 ) ); 
			
			if ( distance1 <= radius1 )
			{
				// draw
				final float newRadius = (float)Math.sqrt( radius1 * radius1 - distance1 * distance1 );
				
				final OvalRoi or = new OvalRoi( Math.round( p1.getL()[ 0 ] - newRadius ), Math.round( p1.getL()[ 1 ] - newRadius ), Math.round( newRadius * 2 ), Math.round( newRadius * 2 ) );
				or.setStrokeColor( color );
				o1.add( or );
			}

			if ( distance2 <= radius2 )
			{
				// draw
				final float newRadius = (float)Math.sqrt( radius2 * radius2 - distance2 * distance2 );

				final OvalRoi or = new OvalRoi( Math.round( p2.getL()[ 0 ] - newRadius ), Math.round( p2.getL()[ 1 ] - newRadius ), Math.round( newRadius * 2 ), Math.round( newRadius * 2 ) );
				or.setStrokeColor( color );
				o2.add( or );
			}
		}
		
		imp1.setOverlay( o1 );
		imp2.setOverlay( o2 );
		
		imp1.updateAndDraw();
		imp2.updateAndDraw();
	}
	
	protected void addCorrespondence( final ExtendedPointMatch pm )
	{
		this.matches.add( pm );
		this.list1.add( getStringForPoint( pm.getP1(), pm.radius1W ) );
		this.list2.add( getStringForPoint( pm.getP2(), pm.radius2W ) );
		
		drawCurrentSelection( standard );
	}
	
	@Override
	public Roi optimizeRoi(Roi roi, ImageProcessor ip) 
	{
		Roi result = new ShapeRoi(roi);
		Roi[] rois = ((ShapeRoi)result).getRois();
		if (rois.length == 1)
			result = rois[0];
		result.setImage(WindowManager.getCurrentImage());
		result.nudge(KeyEvent.VK_RIGHT);
		return result;
	}

	@Override
	public void mouseClicked( final MouseEvent e ) 
	{
		super.mouseClicked( e );
		
		final ImagePlus imp = getImagePlus( e );
		
		if ( imp != imp1 && imp != imp2 )
		{
			e.consume();
			return;
		}
		
		final int x = getOffscreenX( e );
		final int y = getOffscreenY( e );
		
		final int z = imp.getCurrentSlice();
		
		final double xR = imp.getCalibration().getX( x );
		final double yR = imp.getCalibration().getY( y );
		final double zR = imp.getCalibration().getZ( z - 1 );
		
		if ( imp == imp1 )
		{
			if ( p1 == null )
			{
				p1 = new Point( new float[] { (float)x, (float)y, (float)z }, new float[] { (float)xR, (float)yR, (float)zR } );
				addCross( imp, x, y );
			}
			else if ( radius1 == 0 )
			{
				radius1 = Point.distance( p1, new Point( new float[] { (float)xR, (float)yR, (float)zR } ) );
				
				if ( radius1 == 0 )
					radius1 = 0.45f * (float)imp.getCalibration().getX( 1 );
				
				removeColoredOverlays( imp, 2, Color.red );
				addCircle( imp, Math.round( p1.getL()[ 0 ] ), Math.round( p1.getL()[ 1 ] ), (float)radius1 / (float)imp.getCalibration().getX( 1 ), Color.RED );
			}
		}
		else
		{
			if ( p2 == null )
			{
				p2 = new Point( new float[] { (float)x, (float)y, (float)z }, new float[] { (float)xR, (float)yR, (float)zR } );
				addCross( imp, x, y );
			}
			else if ( radius2 == 0 )
			{
				radius2 = Point.distance( p2, new Point( new float[] { (float)xR, (float)yR, (float)zR } ) );
				
				if ( radius2 == 0 )
					radius2 = 0.45f * (float)imp.getCalibration().getX( 1 );
				
				removeColoredOverlays( imp, 2, Color.red );
				addCircle( imp, Math.round( p2.getL()[ 0 ] ), Math.round( p2.getL()[ 1 ] ), (float)radius2 / (float)imp.getCalibration().getX( 1 ) , Color.RED );
			}
		}

		if ( p1 != null && p2 != null && radius1 > 0 && radius2 > 0 )
		{
			addCorrespondence( new ExtendedPointMatch( p1.clone(), p2.clone(), (float)radius1 / (float)imp.getCalibration().getX( 1 ), radius1, (float)radius2 / (float)imp.getCalibration().getX( 1 ), radius2 ) );
			p1 = p2 = null;
			radius1 = radius2 = 0;
			
			// remove temporary overlays
			removeColoredOverlays( imp1, 1, Color.red );
			removeColoredOverlays( imp2, 1, Color.red );
		}
		
		
		//IJ.log("mouse clicked in " + imp.getTitle() + ": (" + x + ", " + y + " " + z + ")" );
		//IJ.log("mouse clicked in " + imp.getTitle() + ": (" + xR + ", " + yR + " " + zR + ")" );
		e.consume(); // prevent ImageJ from handling this event
	}
	
	protected void removeColoredOverlays( final ImagePlus imp, int num, final Color color )
	{
		final Overlay o = imp.getOverlay();
		
		// nothing there
		if ( o == null )
			return;
		
		for ( int i = o.size() - 1; i >=0 && num > 0; --i )
		{
			if ( o.get( i ).getStrokeColor().equals( color ) )
			{
				o.remove( i );
				num--;
			}
		}
		imp.updateAndDraw();
	}

	protected void addCircle( final ImagePlus imp, final int centerX, final int centerY, final float radius, final Color color )
	{
		Overlay o = imp.getOverlay();
		
		if ( o == null )
		{
			o = new Overlay();
			imp.setOverlay( o );
		}

		final OvalRoi or = new OvalRoi( Math.round( centerX - radius ), Math.round( centerY - radius ), Math.round( radius * 2 ), Math.round( radius * 2 ) );
		or.setStrokeColor( color );
		o.add( or );
		
		imp.updateAndDraw();
	}
		
	protected void addCross( final ImagePlus imp, final int x, final int y )
	{
		Overlay o = imp.getOverlay();
		
		if ( o == null )
		{
			o = new Overlay();
			imp.setOverlay( o );
		}
		
		final Line line1 = new Line( x - 3, y, x + 3, y );
		final Line line2 = new Line( x, y - 3, x, y + 3 );
		
		line1.setStrokeColor( Color.red );
		line2.setStrokeColor( Color.red );
		
		o.add( line1 );
		o.add( line2 );
		
		imp.updateAndDraw();
		
		/*
		o.clear();
		
		for ( final DifferenceOfGaussianPeak<FloatType> peak : peaks )
		{
			if ( ( peak.isMax() && lookForMaxima ) || ( peak.isMin() && lookForMinima ) )
			{
				final float x = peak.getPosition( 0 ); 
				final float y = peak.getPosition( 1 );
				
				if ( Math.abs( peak.getValue().get() ) > threshold &&
					 x >= extraSize/2 && y >= extraSize/2 &&
					 x < rect.width+extraSize/2 && y < rect.height+extraSize/2 )
				{
					final OvalRoi or = new OvalRoi( Util.round( x - sigma ) + rect.x - extraSize/2, Util.round( y - sigma ) + rect.y - extraSize/2, Util.round( sigma+sigma2 ), Util.round( sigma+sigma2 ) );
					
					if ( peak.isMax() )
						or.setStrokeColor( Color.green );
					else if ( peak.isMin() )
						or.setStrokeColor( Color.red );
					
					o.add( or );
				}
			}
		}
		*/
		
		
	}

	@Override
	public void mousePressed(MouseEvent e) 
	{
		super.mousePressed(e);
		//IJ.log("mouse pressed: " + getOffscreenX(e) + ", " + getOffscreenY(e));
		e.consume(); // prevent ImageJ from handling this event
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		super.mouseReleased(e);
		//IJ.log("mouse released: " + getOffscreenX(e) + ", " + getOffscreenY(e));
		e.consume(); // prevent ImageJ from handling this event
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		super.mouseEntered(e);
		//IJ.log("mouse entered: " + getOffscreenX(e) + ", " + getOffscreenY(e));
		e.consume(); // prevent ImageJ from handling this event
	}

	@Override
	public void mouseExited(MouseEvent e) {
		super.mouseExited(e);
		//IJ.log("mouse exited: " + getOffscreenX(e) + ", " + getOffscreenY(e));
		e.consume(); // prevent ImageJ from handling this event
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		super.mouseMoved(e);
		//IJ.log("mouse moved: " + getOffscreenX(e) + ", " + getOffscreenY(e));
		e.consume(); // prevent ImageJ from handling this event
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		super.mouseDragged(e);
		//IJ.log("mouse dragged: " + getOffscreenX(e) + ", " + getOffscreenY(e));
		e.consume(); // prevent ImageJ from handling this event
	}

	@Override
	public void sliceChanged(ImagePlus image) 
	{
		super.sliceChanged(image);
		
		drawCurrentSelection( standard );
		
		// reset current selection if slice changed
		p1 = p2 = null;
		radius1 = radius2 = 0;
		
		//IJ.log("slice changed to " + image.getCurrentSlice() + " in " + image.getTitle());
	}

	@Override
	public void showOptionDialog() {
		//GenericDialogPlus gd = new GenericDialogPlus(getToolName() + " Options");
		//gd.addMessage("Here could be your option dialog!");
		//addIOButtons(gd);
		//gd.showDialog();
	}

	@Override
	public void toolToggled(boolean enabled) {
		//IJ.log(getToolName() + " was switched " + (enabled ? "on" : "off"));
	}
	
	public static String getStringForPoint( final Point p, final float r )
	{
		return Util.printCoordinates( p.getW() ) + " [" + Math.round( p.getL()[ 0 ] ) + ", " + Math.round( p.getL()[ 1 ] ) + ", " + Math.round( p.getL()[ 2 ] ) + "], r = " + r;
	}	
}
