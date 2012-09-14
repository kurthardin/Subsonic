package github.daneren2005.dsub.view;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.activity.DownloadActivity;
import github.daneren2005.dsub.domain.MusicDirectory.Entry;
import github.daneren2005.dsub.domain.PlayerState;
import github.daneren2005.dsub.service.DownloadService;
import github.daneren2005.dsub.service.DownloadService.NowPlayingListener;
import github.daneren2005.dsub.util.ImageLoader;
import github.daneren2005.dsub.util.ImageLoader.ImageLoaderTaskHandler;
import github.daneren2005.dsub.util.Util;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class NowPlayingView extends LinearLayout implements NowPlayingListener {

	private final ImageLoader mImageLoader;
	
	private ImageView mAlbumArt;
	private TextView mTitle;
	private TextView mAlbum;
	private TextView mArtist;
	
	private ImageButton mPlayPauseButton;
	
	private boolean mIsPlaying;

	public NowPlayingView(Context context) {
		super(context);
		mImageLoader = new ImageLoader(context);
		inflateView(context);
	}
	
	public NowPlayingView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mImageLoader = new ImageLoader(context);
		inflateView(context);
	}
	
	public void inflateView(final Context context) {
		View nowPlayingView = View.inflate(context, R.layout.now_playing, this);
		nowPlayingView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(context, DownloadActivity.class);
		        context.startActivity(intent);
			}
		});
		
        mAlbumArt = (ImageView) nowPlayingView.findViewById(R.id.now_playing_image);
        mTitle = (TextView) nowPlayingView.findViewById(R.id.now_playing_title);
        mTitle.setSelected(true);
        mAlbum = (TextView) nowPlayingView.findViewById(R.id.now_playing_album);
        mArtist = (TextView) nowPlayingView.findViewById(R.id.now_playing_artist);
        
        ImageButton prevButton = (ImageButton) nowPlayingView.findViewById(R.id.now_playing_previous);
        prevButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Util.getDownloadService(context).previous();
			}
		});
        mPlayPauseButton = (ImageButton) nowPlayingView.findViewById(R.id.now_playing_play_pause);
        mPlayPauseButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mIsPlaying) {
					Util.getDownloadService(context).pause();
				} else {
					Util.getDownloadService(context).start();
				}
			}
		});
        ImageButton nextButton = (ImageButton) nowPlayingView.findViewById(R.id.now_playing_next);
        nextButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Util.getDownloadService(context).next();
			}
		});
        
	}
	
	public CharSequence getTitle() {
		return mTitle.getText();
	}
	
	public void setTitle(String title) {
		mTitle.setText(title);
	}
	
	public CharSequence getAlbum() {
		return mAlbum.getText();
	}
	
	public void setAlbum(String album) {
		mAlbum.setText(album);
	}
	
	public CharSequence getArtist() {
		return mArtist.getText();
	}
	
	public void setArtist(String artist) {
		mArtist.setText(artist);
	}
	
	public Drawable getAlbumArt() {
		return mAlbumArt.getDrawable();
	}
	
	public void setAlbumArt(Drawable albumArt) {
		mAlbumArt.setImageDrawable(albumArt);
	}
	
	public boolean isPlaying() {
		return mIsPlaying;
	}
	
	public boolean isVisible() {
		CharSequence title = mTitle.getText();
		return (title != null && title.length() > 0);
	}

	@Override
	public void onCurrentSongChanged(final DownloadService service, final Entry song) {
		post(new Runnable() {
			@Override
			public void run() {
				if (song == null) {
					mTitle.setText(null);
					mAlbum.setText(null);
					mArtist.setText(null);
					mAlbumArt.setImageDrawable(null);
				} else {
					mTitle.setText(song.getTitle());
					mAlbum.setText(song.getAlbum());
					mArtist.setText(song.getArtist());
					mImageLoader.loadImage(song, false, new ImageLoaderTaskHandler() {
						@Override
						public void done(Drawable drawable) {
							mAlbumArt.setImageDrawable(drawable);
						}
					});
				}
				setVisibility(isVisible() ? View.VISIBLE : View.GONE);
			}
		});
	}

	@Override
	public void onPlaybackStateChanged(final DownloadService service, final PlayerState state) {
		post(new Runnable() {
			@Override
			public void run() {
				mIsPlaying = (state == PlayerState.STARTED || state == PlayerState.DOWNLOADING);
				mPlayPauseButton.setImageResource(mIsPlaying ? 
						R.drawable.ic_media_pause : R.drawable.ic_media_play);
			}
		});
	}

}
