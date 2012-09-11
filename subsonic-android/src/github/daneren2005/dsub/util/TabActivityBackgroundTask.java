package github.daneren2005.dsub.util;

import github.daneren2005.dsub.activity.SubsonicActivity;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public abstract class TabActivityBackgroundTask<T> extends BackgroundTask<T> {

    private final SubsonicActivity tabActivity;

    public TabActivityBackgroundTask(SubsonicActivity activity) {
        super(activity);
        tabActivity = activity;
    }

    @Override
    public void execute() {
        tabActivity.setProgressVisible(true);

        new Thread() {
            @Override
            public void run() {
                try {
                    final T result = doInBackground();
                    if (isCancelled()) {
                        return;
                    }

                    getHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            tabActivity.setProgressVisible(false);
                            done(result);
                        }
                    });
                } catch (final Throwable t) {
                    if (isCancelled()) {
                        return;
                    }
                    getHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            tabActivity.setProgressVisible(false);
                            error(t);
                        }
                    });
                }
            }
        }.start();
    }

    private boolean isCancelled() {
        return tabActivity.isDestroyed();
    }

    @Override
    public void updateProgress(final String message) {
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                tabActivity.updateProgress(message);
            }
        });
    }
}
