package it.polimi.dima.giftlist.presentation.view.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

import icepick.Icepick;
import it.polimi.dima.giftlist.ApplicationComponent;
import it.polimi.dima.giftlist.GiftlistApplication;
import it.polimi.dima.giftlist.presentation.navigation.IntentStarter;

/**
 * Created by Alessandro on 18/03/16.
 */
public class BaseActivity extends AppCompatActivity {

    @Inject
    IntentStarter intentStarter;
    @Inject
    SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        injectDependencies();
        super.onCreate(savedInstanceState);
        Icepick.restoreInstanceState(this, savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Icepick.saveInstanceState(this, outState);
    }

    //TODO: approfondire differenza fra add fragment e replace fragment
    //Attenzione anche alla questione event bus
    //http://stackoverflow.com/questions/18634207/difference-between-add-replace-and-addtobackstack
    protected void addFragment(int containerViewId, Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
        .add(containerViewId, fragment)
        .commit();
    }

    protected void injectDependencies() {
        this.getApplicationComponent().inject(this);
    }

    protected ApplicationComponent getApplicationComponent() {
        return ((GiftlistApplication) getApplication()).getApplicationComponent();
    }
}
