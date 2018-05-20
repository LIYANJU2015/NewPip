package org.schabi.newpipe.views;

import android.graphics.Point;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.github.amlcurran.showcaseview.targets.Target;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

import java.lang.reflect.Field;

/**
 * Created by liyanju on 2018/5/13.
 */

public class CaseViewViewTarget implements Target{

    private ViewTarget target;

    public CaseViewViewTarget(ViewTarget target) {
        this.target = target;
    }

    @Override
    public Point getPoint() {
        return target.getPoint();
    }

    public static ViewTarget navigationButtonViewTarget(Toolbar toolbar) {
        try {
            Field field = Toolbar.class.getDeclaredField("mNavButtonView");
            field.setAccessible(true);
            View navigationView = (View) field.get(toolbar);
            return new ViewTarget(navigationView);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }
}
