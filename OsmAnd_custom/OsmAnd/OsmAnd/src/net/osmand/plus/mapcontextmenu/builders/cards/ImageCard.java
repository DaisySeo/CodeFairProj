package net.osmand.plus.mapcontextmenu.builders.cards;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.TrafficStats;
import android.os.AsyncTask;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;

import net.osmand.AndroidNetworkUtils;
import net.osmand.AndroidUtils;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapillary.MapillaryContributeCard;
import net.osmand.plus.mapillary.MapillaryImageCard;
import net.osmand.plus.openplacereviews.OPRWebviewActivity;
import net.osmand.plus.wikimedia.WikiImageHelper;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public abstract class ImageCard extends AbstractCard {

	public static String TYPE_MAPILLARY_PHOTO = "mapillary-photo";
	public static String TYPE_MAPILLARY_CONTRIBUTE = "mapillary-contribute";

	private static final Log LOG = PlatformUtil.getLog(ImageCard.class);
	protected String type;
	// Image location
	protected LatLon location;
	// (optional) Image's camera angle in range  [0, 360]
	protected double ca = Double.NaN;
	// Date When bitmap was captured
	protected Date timestamp;
	// Image key
	protected String key;
	// Image title
	protected String title;
	// User name
	protected String userName;
	// Image viewer url
	protected String url;
	// Image bitmap url
	protected String imageUrl;
	// Image high resolution bitmap url
	protected String imageHiresUrl;
	// true if external browser should to be opened, open webview otherwise
	protected boolean externalLink;

	protected int topIconId;
	protected int buttonIconId;
	protected String buttonText;
	protected int buttonIconColor;
	protected int buttonColor;
	protected int buttonTextColor;

	private int defaultCardLayoutId = R.layout.context_menu_card_image;

	protected Drawable icon;
	protected Drawable buttonIcon;
	protected OnClickListener onClickListener;
	protected OnClickListener onButtonClickListener;

	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);

	private boolean downloading;
	private boolean downloaded;
	private Bitmap bitmap;
	private float bearingDiff = Float.NaN;
	private float distance = Float.NaN;

	public ImageCard(MapActivity mapActivity, JSONObject imageObject) {
		super(mapActivity);
		if (imageObject != null) {
			try {
				if (imageObject.has("type")) {
					this.type = imageObject.getString("type");
				}
				if (imageObject.has("ca") && !imageObject.isNull("ca")) {
					this.ca = imageObject.getDouble("ca");
				}
				if (imageObject.has("lat") && imageObject.has("lon")
						&& !imageObject.isNull("lat") && !imageObject.isNull("lon")) {
					double latitude = imageObject.getDouble("lat");
					double longitude = imageObject.getDouble("lon");
					this.location = new LatLon(latitude, longitude);
				}
				if (imageObject.has("timestamp")) {
					try {
						this.timestamp = DATE_FORMAT.parse(imageObject.getString("timestamp"));
					} catch (ParseException e) {
						e.printStackTrace();
					}
				}
				if (imageObject.has("key")) {
					this.key = imageObject.getString("key");
				}
				if (imageObject.has("title") && !imageObject.isNull("title")) {
					this.title = imageObject.getString("title");
				}
				if (imageObject.has("username") && !imageObject.isNull("username")) {
					this.userName = imageObject.getString("username");
				}
				if (imageObject.has("url") && !imageObject.isNull("url")) {
					this.url = imageObject.getString("url");
				}
				if (imageObject.has("imageUrl") && !imageObject.isNull("imageUrl")) {
					this.imageUrl = imageObject.getString("imageUrl");
				}
				if (imageObject.has("imageHiresUrl") && !imageObject.isNull("imageHiresUrl")) {
					this.imageHiresUrl = imageObject.getString("imageHiresUrl");
				}
				if (imageObject.has("externalLink") && !imageObject.isNull("externalLink")) {
					this.externalLink = imageObject.getBoolean("externalLink");
				}
				if (imageObject.has("topIcon") && !imageObject.isNull("topIcon")) {
					this.topIconId = AndroidUtils.getDrawableId(getMyApplication(), imageObject.getString("topIcon"));
				}
				if (imageObject.has("buttonIcon") && !imageObject.isNull("buttonIcon")) {
					this.buttonIconId = AndroidUtils.getDrawableId(getMyApplication(), imageObject.getString("buttonIcon"));
				}
				if (imageObject.has("buttonText") && !imageObject.isNull("buttonText")) {
					this.buttonText = imageObject.getString("buttonText");
				}
				if (imageObject.has("buttonIconColor") && !imageObject.isNull("buttonIconColor")) {
					try {
						this.buttonIconColor = Algorithms.parseColor(imageObject.getString("buttonIconColor"));
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					}
				}
				if (imageObject.has("buttonColor") && !imageObject.isNull("buttonColor")) {
					try {
						this.buttonColor = Algorithms.parseColor(imageObject.getString("buttonColor"));
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					}
				}
				if (imageObject.has("buttonTextColor") && !imageObject.isNull("buttonTextColor")) {
					try {
						this.buttonTextColor = Algorithms.parseColor(imageObject.getString("buttonTextColor"));
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static ImageCard createCard(MapActivity mapActivity, JSONObject imageObject) {
		ImageCard imageCard = null;
		try {
			if (imageObject.has("type")) {
				String type = imageObject.getString("type");
				if (TYPE_MAPILLARY_PHOTO.equals(type)) {
					imageCard = new MapillaryImageCard(mapActivity, imageObject);
				} else if (TYPE_MAPILLARY_CONTRIBUTE.equals(type)) {
					imageCard = new MapillaryContributeCard(mapActivity, imageObject);
				} else {
					imageCard = new UrlImageCard(mapActivity, imageObject);
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return imageCard;
	}

	private static ImageCard createCardOpr(MapActivity mapActivity, JSONObject imageObject) {
		ImageCard imageCard = null;
		if (imageObject.has("cid")) {
			imageCard = new IPFSImageCard(mapActivity, imageObject);
		}
		return imageCard;
	}

	public double getCa() {
		return ca;
	}

	public String getKey() {
		return key;
	}

	public String getTitle() {
		return title;
	}

	public String getType() {
		return type;
	}

	public LatLon getLocation() {
		return location;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public String getUserName() {
		return userName;
	}

	public String getUrl() {
		return url;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public String getImageHiresUrl() {
		return imageHiresUrl;
	}

	public boolean isExternalLink() {
		return externalLink;
	}

	public int getTopIconId() {
		return topIconId;
	}

	public int getButtonIconId() {
		return buttonIconId;
	}

	public String getButtonText() {
		return buttonText;
	}

	public int getButtonIconColor() {
		return buttonIconColor;
	}

	public int getButtonColor() {
		return buttonColor;
	}

	public int getButtonTextColor() {
		return buttonTextColor;
	}

	public int getDefaultCardLayoutId() {
		return defaultCardLayoutId;
	}

	@Override
	public int getCardLayoutId() {
		return defaultCardLayoutId;
	}

	public Drawable getIcon() {
		return icon;
	}

	public OnClickListener getOnClickListener() {
		return onClickListener;
	}


	public boolean isDownloading() {
		return downloading;
	}

	public void setDownloading(boolean downloading) {
		this.downloading = downloading;
	}

	public Bitmap getBitmap() {
		return bitmap;
	}

	public void setBitmap(Bitmap bitmap) {
		this.bitmap = bitmap;
	}

	public float getBearingDiff() {
		return bearingDiff;
	}

	public void setBearingDiff(float bearingDiff) {
		this.bearingDiff = bearingDiff;
	}

	public float getDistance() {
		return distance;
	}

	public void setDistance(float distance) {
		this.distance = distance;
	}

	public void update() {
		if (view != null) {
			ImageView image = (ImageView) view.findViewById(R.id.image);
			ImageView iconImageView = (ImageView) view.findViewById(R.id.icon);
			TextView urlTextView = (TextView) view.findViewById(R.id.url);
			TextView watermarkTextView = (TextView) view.findViewById(R.id.watermark);
			ProgressBar progress = (ProgressBar) view.findViewById(R.id.progress);
			AppCompatButton button = (AppCompatButton) view.findViewById(R.id.button);

			boolean night = getMyApplication().getDaynightHelper().isNightModeForMapControls();
			AndroidUtils.setBackground(getMapActivity(), view.findViewById(R.id.card_background), night,
					R.drawable.context_menu_card_light, R.drawable.context_menu_card_dark);

			if (icon == null && topIconId != 0) {
				icon = getMyApplication().getUIUtilities().getIcon(topIconId);
			}
			if (icon == null) {
				iconImageView.setVisibility(View.GONE);
			} else {
				iconImageView.setImageDrawable(icon);
				iconImageView.setVisibility(View.VISIBLE);
			}
			if (Algorithms.isEmpty(userName)) {
				watermarkTextView.setVisibility(View.GONE);
			} else {
				watermarkTextView.setText("@" + userName);
				watermarkTextView.setVisibility(View.VISIBLE);
			}
			if (downloading) {
				progress.setVisibility(View.VISIBLE);
				image.setImageBitmap(null);
			} else if (!downloaded) {
				MenuBuilder.execute(new DownloadImageTask());
			} else {
				progress.setVisibility(View.GONE);
				image.setImageBitmap(bitmap);
				if (bitmap == null) {
					urlTextView.setVisibility(View.VISIBLE);
					urlTextView.setText(getUrl());
				} else {
					urlTextView.setVisibility(View.GONE);
				}
			}
			if (onClickListener != null) {
				view.findViewById(R.id.image_card).setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						onClickListener.onClick(v);
					}
				});
			} else {
				view.findViewById(R.id.image_card).setOnClickListener(null);
			}

			if (!Algorithms.isEmpty(buttonText)) {
				button.setText(buttonText);
			}
			if (buttonIcon == null && buttonIconId != 0) {
				if (buttonIconColor != 0) {
					buttonIcon = getMyApplication().getUIUtilities().getPaintedIcon(buttonIconId, buttonIconColor);
				} else {
					buttonIcon = getMyApplication().getUIUtilities().getIcon(buttonIconId);
				}
			}
			button.setCompoundDrawablesWithIntrinsicBounds(buttonIcon, null, null, null);
			if (buttonColor != 0) {
				button.setSupportBackgroundTintList(ColorStateList.valueOf(buttonColor));
			}
			if (buttonTextColor != 0) {
				button.setTextColor(buttonTextColor);
			}
			if (onButtonClickListener != null) {
				button.setVisibility(View.VISIBLE);
				button.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						onButtonClickListener.onClick(v);
					}
				});
			} else {
				button.setVisibility(View.GONE);
				button.setOnClickListener(null);
			}
		}
	}

	private static String[] getIdFromResponse(String response) {
		try {
			JSONArray obj = new JSONObject(response).getJSONArray("objects");
			JSONArray images = (JSONArray) ((JSONObject) obj.get(0)).get("id");
			return toStringArray(images);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return new String[0];
	}

	private static String[] toStringArray(JSONArray array) {
		if (array == null)
			return null;

		String[] arr = new String[array.length()];
		for (int i = 0; i < arr.length; i++) {
			arr[i] = array.optString(i);
		}
		return arr;
	}

	public static class GetImageCardsTask extends AsyncTask<Void, Void, List<ImageCard>> {

		private MapActivity mapActivity;
		private OsmandApplication app;
		private LatLon latLon;
		private Map<String, String> params;
		private GetImageCardsListener listener;
		private List<ImageCard> result;
		private static final int GET_IMAGE_CARD_THREAD_ID = 10104;

		public interface GetImageCardsListener {
			void onPostProcess(List<ImageCard> cardList);

			void onPlaceIdAcquired(String[] placeId);

			void onFinish(List<ImageCard> cardList);
		}

		public GetImageCardsTask(@NonNull MapActivity mapActivity, LatLon latLon,
		                         @Nullable Map<String, String> params, GetImageCardsListener listener) {
			this.mapActivity = mapActivity;
			this.app = mapActivity.getMyApplication();
			this.latLon = latLon;
			this.params = params;
			this.listener = listener;
		}

		@Override
		protected List<ImageCard> doInBackground(Void... params) {
			TrafficStats.setThreadStatsTag(GET_IMAGE_CARD_THREAD_ID);
			List<ImageCard> result = new ArrayList<>();
			Object o = mapActivity.getMapLayers().getContextMenuLayer().getSelectedObject();
			if (o instanceof Amenity) {
				Amenity am = (Amenity) o;
				long amenityId = am.getId() >> 1;
				String baseUrl = OPRWebviewActivity.getBaseUrl(app);
				String url = baseUrl + "api/objects-by-index?type=opr.place&index=osmid&key=" + amenityId;
				String response = AndroidNetworkUtils.sendRequest(app, url, Collections.<String, String>emptyMap(),
						"Requesting location images...", false, false);
				if (response != null) {
					getPicturesForPlace(result, response);
					String[] id = getIdFromResponse(response);
					listener.onPlaceIdAcquired(id);
				}
			}
			try {
				final Map<String, String> pms = new LinkedHashMap<>();
				pms.put("lat", "" + (float) latLon.getLatitude());
				pms.put("lon", "" + (float) latLon.getLongitude());
				Location myLocation = app.getLocationProvider().getLastKnownLocation();
				if (myLocation != null) {
					pms.put("mloc", "" + (float) myLocation.getLatitude() + "," + (float) myLocation.getLongitude());
				}
				pms.put("app", Version.isPaidVersion(app) ? "paid" : "free");
				String preferredLang = app.getSettings().MAP_PREFERRED_LOCALE.get();
				if (Algorithms.isEmpty(preferredLang)) {
					preferredLang = app.getLanguage();
				}
				if (!Algorithms.isEmpty(preferredLang)) {
					pms.put("lang", preferredLang);
				}
				if (this.params != null) {
					String wikidataId = this.params.get(Amenity.WIKIDATA);
					if (wikidataId != null) {
						this.params.remove(Amenity.WIKIDATA);
						WikiImageHelper.addWikidataImageCards(mapActivity, wikidataId, result);
					}
					String wikimediaContent = this.params.get(Amenity.WIKIMEDIA_COMMONS);
					if (wikimediaContent != null) {
						this.params.remove(Amenity.WIKIMEDIA_COMMONS);
						WikiImageHelper.addWikimediaImageCards(mapActivity, wikimediaContent, result);
					}
					pms.putAll(this.params);
				}
				String response = AndroidNetworkUtils.sendRequest(app, "https://osmand.net/api/cm_place", pms,
						"Requesting location images...", false, false);

				if (!Algorithms.isEmpty(response)) {
					JSONObject obj = new JSONObject(response);
					JSONArray images = obj.getJSONArray("features");
					if (images.length() > 0) {
						for (int i = 0; i < images.length(); i++) {
							try {
								JSONObject imageObject = (JSONObject) images.get(i);
								if (imageObject != JSONObject.NULL) {
									ImageCard imageCard = ImageCard.createCard(mapActivity, imageObject);
									if (imageCard != null) {
										result.add(imageCard);
									}
								}
							} catch (JSONException e) {
								e.printStackTrace();
							}
						}
					}
				}
			} catch (Exception e) {
				LOG.error(e);
			}
			if (listener != null) {
				listener.onPostProcess(result);
			}
			return result;
		}

		private void getPicturesForPlace(List<ImageCard> result, String response) {
			try {
				if (!Algorithms.isEmpty(response)) {
					JSONArray obj = new JSONObject(response).getJSONArray("objects");
					JSONObject imagesWrapper = ((JSONObject) ((JSONObject) obj.get(0)).get("images"));
					Iterator<String> it = imagesWrapper.keys();
					while (it.hasNext()) {
						JSONArray images = imagesWrapper.getJSONArray(it.next());
						if (images.length() > 0) {
							for (int i = 0; i < images.length(); i++) {
								try {
									JSONObject imageObject = (JSONObject) images.get(i);
									if (imageObject != JSONObject.NULL) {
										ImageCard imageCard = ImageCard.createCardOpr(mapActivity, imageObject);
										if (imageCard != null) {
											result.add(imageCard);
										}
									}
								} catch (JSONException e) {
									LOG.error(e);
								}
							}
						}
					}
				}
			} catch (Exception e) {
				LOG.error(e);
			}
		}

		@Override
		protected void onPostExecute(List<ImageCard> cardList) {
			result = cardList;
			if (listener != null) {
				listener.onFinish(result);
			}
		}
	}

	private class DownloadImageTask extends AsyncTask<Void, Void, Bitmap> {

		@Override
		protected void onPreExecute() {
			downloading = true;
			update();
		}

		@Override
		protected Bitmap doInBackground(Void... params) {
			return AndroidNetworkUtils.downloadImage(getMyApplication(), imageUrl);
		}

		@Override
		protected void onPostExecute(Bitmap bitmap) {
			downloading = false;
			downloaded = true;
			ImageCard.this.bitmap = bitmap;
			if (bitmap != null && Algorithms.isEmpty(getImageHiresUrl())) {
				ImageCard.this.imageHiresUrl = getUrl();
			}
			update();
		}
	}
}