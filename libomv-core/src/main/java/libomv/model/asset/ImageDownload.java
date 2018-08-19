package libomv.model.asset;

import libomv.imaging.ManagedImage.ImageCodec;
import libomv.model.texture.TextureRequestState;
import libomv.utils.CallbackHandler;

public class ImageDownload extends Transfer {
	public ImageType imageType;
	public ImageCodec codec;
	public int discardLevel;
	public float priority;
	// The current {@link TextureRequestState} which identifies the current
	// status of the request
	public TextureRequestState state;
	// If true, indicates the callback will be fired whenever new data is
	// returned from the simulator.
	// This is used to progressively render textures as portions of the
	// texture are received.
	public boolean reportProgress;
	// The callback to fire when the request is complete, will include
	// the {@link TextureRequestState} and the <see cref="AssetTexture"/>
	// object containing the result data
	public CallbackHandler<ImageDownload> callbacks;

	public ImageDownload() {
		super();
	}

	public boolean gotInfo() {
		return size > 0;
	}
}