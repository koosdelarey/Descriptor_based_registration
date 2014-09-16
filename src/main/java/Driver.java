import net.imagej.patcher.LegacyInjector;
import fiji.Debug;
public class Driver {
	
	static {
		LegacyInjector.preinit();
	}	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Debug.run("Descriptor-based registration (2d/3d)","");	
	}
}