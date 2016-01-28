package it.polimi.dima.giftlist.product;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import it.polimi.dima.giftlist.DummyInterface;
import it.polimi.dima.giftlist.DummyList;
import it.polimi.dima.giftlist.base.Repository;
import it.polimi.dima.giftlist.product.Rest.EtsyRestDataSource;
import it.polimi.dima.giftlist.util.ErrorMessageDeterminer;

/**
 * Created by Elena on 27/01/2016.
 */
@Module()
public class ProductModule {

    private Context context;

    public ProductModule(Context context) {
        this.context = context;
    }

    @Provides
    public Context provideContext() {
        return context;
    }

    @Provides
    @Singleton
    public DummyInterface providesDummyInterface() {
        return new DummyList();
    }

    @Provides
    @Singleton
    public Repository providesRepository() {
        return new EtsyRestDataSource();
    }

    @Provides
    @Singleton
    public ErrorMessageDeterminer providesErrorMessageDeterminer(){
        return new ErrorMessageDeterminer();
    }

}
