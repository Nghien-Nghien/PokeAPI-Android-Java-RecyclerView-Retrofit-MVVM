package com.example.pokemonapi.repository;

import android.annotation.SuppressLint;

import com.example.pokemonapi.database.DatabaseBuilder;
import com.example.pokemonapi.database.PokemonInfoDAO;
import com.example.pokemonapi.model.pokemoninfo.PokemonInfoAPI;
import com.example.pokemonapi.model.pokemoninfo.TypesResponse;
import com.example.pokemonapi.network.APIClient;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class DetailPresenter implements Contracts.DetailPresenter {

    private Contracts.DetailView detailView;
    private APIClient apiClient;
    private PokemonInfoDAO pokemonInfoDAO;
    private List<TypesResponse> typesData;
    private Disposable disposable;

    public DetailPresenter(Contracts.DetailView detailView) {
        this.detailView = detailView;
        pokemonInfoDAO = DatabaseBuilder.getINSTANCE().databaseBuilder().pokemonInfoDAO();
    }

    @Inject
    public DetailPresenter(APIClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public void fetchPokemonInfo(String namePoke) {
        //pokemonInfoDAO.deleteAll(); // use to clear old database
        detailView.showProgressBar();
        typesData = new ArrayList<>();

        Observable<PokemonInfoAPI> pokemonInfoAPIObservable = apiClient.observableFetchPokemonInfo(namePoke);
        Observer<PokemonInfoAPI> pokemonInfoAPIObserver = getPokemonInfoAPIObserver(namePoke);

        pokemonInfoAPIObservable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pokemonInfoAPIObserver);
    }

    private Observer<PokemonInfoAPI> getPokemonInfoAPIObserver(String namePoke) {
        return new Observer<PokemonInfoAPI>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
                disposable = d;
            }

            @Override
            public void onNext(@NonNull PokemonInfoAPI pokemonInfoAPI) {
                onResponseSuccess(pokemonInfoAPI, namePoke);
            }

            @Override
            public void onError(@NonNull Throwable e) {
                onResponseFail(e, namePoke);
            }

            @Override
            public void onComplete() {

            }
        };
    }

    private void onResponseSuccess(PokemonInfoAPI pokemonInfoAPI, String namePoke) {
        //Get these info: weight, height
        @SuppressLint("DefaultLocale") String heightFormatted = String.format("%.1f M", (float) pokemonInfoAPI.getHeight() / 10);
        @SuppressLint("DefaultLocale") String weightFormatted = String.format("%.1f KG", (float) pokemonInfoAPI.getWeight() / 10);

        //Get Base Performance
        Float hpFormatted = (float) pokemonInfoAPI.hp;
        Float atkFormatted = (float) pokemonInfoAPI.atk;
        Float defFormatted = (float) pokemonInfoAPI.def;
        Float spdFormatted = (float) pokemonInfoAPI.spd;
        Float expFormatted = (float) pokemonInfoAPI.exp;

        String hpString = pokemonInfoAPI.hpString;
        String atkString = pokemonInfoAPI.atkString;
        String defString = pokemonInfoAPI.defString;
        String spdString = pokemonInfoAPI.spdString;
        String expString = pokemonInfoAPI.expString;

        //Get name of types Pokemon and Color Types
        List<TypesResponse> typesList = pokemonInfoAPI.getTypes();
        for (int i = 0; i < typesList.size(); i++) {
            TypesResponse type = typesList.get(i);

            String nameType = type.getType().getName();

            typesData.add(new TypesResponse(nameType));
        }

        detailView.hideProgressBar();
        detailView.onOnlineResponse(typesData, heightFormatted, weightFormatted,
                hpFormatted, atkFormatted, defFormatted, spdFormatted, expFormatted,
                hpString, atkString, defString, spdString, expString);
        pokemonInfoDAO.insertPokemonInfo(new PokemonInfoAPI(namePoke, typesData, heightFormatted, weightFormatted,
                hpFormatted, atkFormatted, defFormatted, spdFormatted, expFormatted,
                hpString, atkString, defString, spdString, expString));
    }

    private void onResponseFail(Throwable e, String namePoke) {
        detailView.hideProgressBar();
        detailView.onFailure(e.toString());

        if (pokemonInfoDAO.getPokemonInfo(namePoke) != null) {
            detailView.onOfflineResponse(pokemonInfoDAO.getPokemonInfo(namePoke));
            detailView.toastForOfflineMode();
        }
    }

    public void getDisposableToUnsubscribe() {
        disposable.dispose();
    }
}
