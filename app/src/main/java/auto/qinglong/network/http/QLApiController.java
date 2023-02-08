package auto.qinglong.network.http;

import androidx.annotation.NonNull;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Objects;

import auto.qinglong.bean.app.Account;
import auto.qinglong.bean.ql.QLDependence;
import auto.qinglong.bean.ql.QLEnvironment;
import auto.qinglong.bean.ql.QLLog;
import auto.qinglong.bean.ql.QLLoginLog;
import auto.qinglong.bean.ql.QLScript;
import auto.qinglong.bean.ql.QLTask;
import auto.qinglong.bean.ql.network.QLBaseRes;
import auto.qinglong.bean.ql.network.QLDependenceRes;
import auto.qinglong.bean.ql.network.QLDependenciesRes;
import auto.qinglong.bean.ql.network.QLEnvEditRes;
import auto.qinglong.bean.ql.network.QLEnvironmentRes;
import auto.qinglong.bean.ql.network.QLLogRemoveRes;
import auto.qinglong.bean.ql.network.QLLoginLogsRes;
import auto.qinglong.bean.ql.network.QLLoginRes;
import auto.qinglong.bean.ql.network.QLLogsRes;
import auto.qinglong.bean.ql.network.QLScriptsRes;
import auto.qinglong.bean.ql.network.QLSystemRes;
import auto.qinglong.bean.ql.network.QLTaskEditRes;
import auto.qinglong.bean.ql.network.QLTasksRes;
import auto.qinglong.bean.ql.network.QLTextRes;
import auto.qinglong.database.sp.AccountSP;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * QL API统一请求类
 */
public class QLApiController {
    private static final String ERROR_NO_BODY = "响应异常";
    private static final String ERROR_INVALID_AUTH = "登录失效";

    public static void getSystemInfo(@NonNull String requestId, @NonNull Account account, @NonNull NetSystemCallback callback) {
        Call<QLSystemRes> call = new Retrofit.Builder()
                .baseUrl(account.getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(QLApi.class)
                .getSystemInfo();

        call.enqueue(new Callback<QLSystemRes>() {
            @Override
            public void onResponse(Call<QLSystemRes> call, Response<QLSystemRes> response) {
                RequestManager.finishCall(requestId);
                QLSystemRes res = response.body();
                if (res == null) {
                    if (response.code() == 401) {
                        callback.onFailure(ERROR_INVALID_AUTH);
                    } else {
                        callback.onFailure(ERROR_NO_BODY + response.code());
                    }
                } else {
                    if (res.getCode() == 200) {
                        callback.onSuccess(res);
                    } else {
                        callback.onFailure(res.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<QLSystemRes> call, Throwable t) {
                RequestManager.finishCall(requestId);
                if (call.isCanceled()) {
                    return;
                }
                callback.onFailure(t.getLocalizedMessage());
            }
        });

        RequestManager.addCall(call, requestId);
    }

    public static void checkToken(@NonNull String requestId, @NonNull Account account, @NonNull NetBaseCallback callback) {
        Call<QLLogRemoveRes> call = new Retrofit.Builder()
                .baseUrl(account.getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(QLApi.class)
                .getLogRemove(account.getAuthorization());

        call.enqueue(new Callback<QLLogRemoveRes>() {
            @Override
            public void onResponse(Call<QLLogRemoveRes> call, Response<QLLogRemoveRes> response) {
                RequestManager.finishCall(requestId);
                QLLogRemoveRes res = response.body();
                if (res == null) {
                    if (response.code() == 401) {
                        callback.onFailure(ERROR_INVALID_AUTH);
                    } else {
                        callback.onFailure(ERROR_NO_BODY + response.code());
                    }
                } else {
                    if (res.getCode() == 200) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure(res.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<QLLogRemoveRes> call, Throwable t) {
                RequestManager.finishCall(requestId);
                if (call.isCanceled()) {
                    return;
                }
                callback.onFailure(t.getLocalizedMessage());
            }
        });

        RequestManager.addCall(call, requestId);
    }

    public static void login(@NonNull String requestId, @NonNull Account account, @NonNull NetLoginCallback callback) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("username", account.getUsername());
        jsonObject.addProperty("password", account.getPassword());
        String json = jsonObject.toString();
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);

        Call<QLLoginRes> call = new Retrofit.Builder()
                .baseUrl(account.getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(QLApi.class)
                .login(body);

        call.enqueue(new Callback<QLLoginRes>() {
            @Override
            public void onResponse(Call<QLLoginRes> call, Response<QLLoginRes> response) {
                RequestManager.finishCall(requestId);
                QLLoginRes res = response.body();
                if (res == null) {
                    if (response.code() == 401) {
                        callback.onFailure(ERROR_INVALID_AUTH);
                    } else {
                        callback.onFailure(ERROR_NO_BODY);
                    }
                } else {
                    if (res.getCode() == 200) {
                        //设置会话信息
                        account.setToken(res.getData().getToken());
                        callback.onSuccess(account);
                    } else {
                        callback.onFailure(res.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<QLLoginRes> call, Throwable t) {
                RequestManager.finishCall(requestId);
                if (call.isCanceled()) {
                    return;
                }
                callback.onFailure(t.getLocalizedMessage());
            }
        });
        RequestManager.addCall(call, requestId);
    }

    public static void getTasks(@NonNull String requestId, @NonNull String searchValue, @NonNull NetGetTasksCallback callback) {
        Call<QLTasksRes> call = new Retrofit.Builder()
                .baseUrl(AccountSP.getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(QLApi.class)
                .getTasks(AccountSP.getAuthorization(), searchValue);
        call.enqueue(new Callback<QLTasksRes>() {
            @Override
            public void onResponse(Call<QLTasksRes> call, Response<QLTasksRes> response) {
                RequestManager.finishCall(requestId);
                QLTasksRes res = response.body();
                if (res == null) {
                    if (response.code() == 401) {
                        callback.onFailure(ERROR_INVALID_AUTH);
                    } else {
                        callback.onFailure(ERROR_NO_BODY);
                    }
                } else {
                    if (res.getCode() == 200) {
                        callback.onSuccess(res);
                    } else {
                        callback.onFailure(res.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<QLTasksRes> call, Throwable t) {
                RequestManager.finishCall(requestId);
                if (call.isCanceled()) {
                    return;
                }
                callback.onFailure(t.getLocalizedMessage());
            }
        });

        RequestManager.addCall(call, requestId);
    }

    public static void runTasks(@NonNull String requestId, @NonNull List<String> taskIds, @NonNull NetBaseCallback callback) {
        JsonArray jsonArray = new JsonArray();
        for (int i = 0; i < taskIds.size(); i++) {
            jsonArray.add(taskIds.get(i));
        }

        String json = jsonArray.toString();
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
        Call<QLBaseRes> call = new Retrofit.Builder()
                .baseUrl(AccountSP.getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(QLApi.class)
                .runTasks(AccountSP.getAuthorization(), body);

        call.enqueue(new Callback<QLBaseRes>() {
            @Override
            public void onResponse(@NonNull Call<QLBaseRes> call, @NonNull Response<QLBaseRes> response) {
                RequestManager.finishCall(requestId);
                QLBaseRes res = response.body();
                if (res == null) {
                    if (response.code() == 401) {
                        callback.onFailure(ERROR_INVALID_AUTH);
                    } else {
                        callback.onFailure(ERROR_NO_BODY);
                    }
                } else {
                    if (res.getCode() == 200) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure(res.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<QLBaseRes> call, @NonNull Throwable t) {
                RequestManager.finishCall(requestId);
                if (call.isCanceled()) {
                    return;
                }
                callback.onFailure(t.getLocalizedMessage());
            }
        });

        RequestManager.addCall(call, requestId);

    }

    public static void stopTasks(@NonNull String requestId, @NonNull List<String> taskIds, @NonNull NetBaseCallback callback) {
        JsonArray jsonArray = new JsonArray();
        for (int i = 0; i < taskIds.size(); i++) {
            jsonArray.add(taskIds.get(i));
        }

        String json = jsonArray.toString();
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
        Call<QLBaseRes> call = new Retrofit.Builder()
                .baseUrl(AccountSP.getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(QLApi.class)
                .stopTasks(AccountSP.getAuthorization(), body);

        call.enqueue(new Callback<QLBaseRes>() {
            @Override
            public void onResponse(@NonNull Call<QLBaseRes> call, @NonNull Response<QLBaseRes> response) {
                RequestManager.finishCall(requestId);
                QLBaseRes res = response.body();
                if (res == null) {
                    if (response.code() == 401) {
                        callback.onFailure(ERROR_INVALID_AUTH);
                    } else {
                        callback.onFailure(ERROR_NO_BODY);
                    }
                } else {
                    if (res.getCode() == 200) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure(res.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<QLBaseRes> call, @NonNull Throwable t) {
                RequestManager.finishCall(requestId);
                if (call.isCanceled()) {
                    return;
                }
                callback.onFailure(t.getLocalizedMessage());
            }
        });

        RequestManager.addCall(call, requestId);

    }

    public static void enableTasks(@NonNull String requestId, @NonNull List<String> taskIds, @NonNull NetBaseCallback callback) {
        JsonArray jsonArray = new JsonArray();
        for (int i = 0; i < taskIds.size(); i++) {
            jsonArray.add(taskIds.get(i));
        }

        String json = jsonArray.toString();
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
        Call<QLBaseRes> call = new Retrofit.Builder()
                .baseUrl(AccountSP.getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(QLApi.class)
                .enableTasks(AccountSP.getAuthorization(), body);

        call.enqueue(new Callback<QLBaseRes>() {
            @Override
            public void onResponse(@NonNull Call<QLBaseRes> call, @NonNull Response<QLBaseRes> response) {
                RequestManager.finishCall(requestId);
                QLBaseRes res = response.body();
                if (res == null) {
                    if (response.code() == 401) {
                        callback.onFailure(ERROR_INVALID_AUTH);
                    } else {
                        callback.onFailure(ERROR_NO_BODY + response.code());
                    }
                } else {
                    if (res.getCode() == 200) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure(res.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<QLBaseRes> call, @NonNull Throwable t) {
                RequestManager.finishCall(requestId);
                if (call.isCanceled()) {
                    return;
                }
                callback.onFailure(t.getLocalizedMessage());
            }
        });

        RequestManager.addCall(call, requestId);

    }

    public static void disableTasks(@NonNull String requestId, @NonNull List<String> taskIds, @NonNull NetBaseCallback callback) {
        JsonArray jsonArray = new JsonArray();
        for (int i = 0; i < taskIds.size(); i++) {
            jsonArray.add(taskIds.get(i));
        }

        String json = jsonArray.toString();
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
        Call<QLBaseRes> call = new Retrofit.Builder()
                .baseUrl(AccountSP.getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(QLApi.class)
                .disableTasks(AccountSP.getAuthorization(), body);

        call.enqueue(new Callback<QLBaseRes>() {
            @Override
            public void onResponse(Call<QLBaseRes> call, Response<QLBaseRes> response) {
                RequestManager.finishCall(requestId);
                QLBaseRes res = response.body();
                if (res == null) {
                    if (response.code() == 401) {
                        callback.onFailure(ERROR_INVALID_AUTH);
                    } else {
                        callback.onFailure(ERROR_NO_BODY + response.code());
                    }
                } else {
                    if (res.getCode() == 200) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure(res.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<QLBaseRes> call, Throwable t) {
                RequestManager.finishCall(requestId);
                if (call.isCanceled()) {
                    return;
                }
                callback.onFailure(t.getLocalizedMessage());
            }
        });

        RequestManager.addCall(call, requestId);

    }

    public static void pinTasks(@NonNull String requestId, @NonNull List<String> taskIds, @NonNull NetBaseCallback callback) {
        JsonArray jsonArray = new JsonArray();
        for (int i = 0; i < taskIds.size(); i++) {
            jsonArray.add(taskIds.get(i));
        }

        String json = jsonArray.toString();
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
        Call<QLBaseRes> call = new Retrofit.Builder()
                .baseUrl(AccountSP.getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(QLApi.class)
                .pinTasks(AccountSP.getAuthorization(), body);

        call.enqueue(new Callback<QLBaseRes>() {
            @Override
            public void onResponse(Call<QLBaseRes> call, Response<QLBaseRes> response) {
                RequestManager.finishCall(requestId);
                QLBaseRes res = response.body();
                if (res == null) {
                    if (response.code() == 401) {
                        callback.onFailure(ERROR_INVALID_AUTH);
                    } else {
                        callback.onFailure(ERROR_NO_BODY + response.code());
                    }
                } else {
                    if (res.getCode() == 200) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure(res.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<QLBaseRes> call, Throwable t) {
                RequestManager.finishCall(requestId);
                if (call.isCanceled()) {
                    return;
                }
                callback.onFailure(t.getLocalizedMessage());
            }
        });

        RequestManager.addCall(call, requestId);

    }

    public static void unpinTasks(@NonNull String requestId, @NonNull List<String> taskIds, @NonNull NetBaseCallback callback) {
        JsonArray jsonArray = new JsonArray();
        for (int i = 0; i < taskIds.size(); i++) {
            jsonArray.add(taskIds.get(i));
        }

        String json = jsonArray.toString();
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
        Call<QLBaseRes> call = new Retrofit.Builder()
                .baseUrl(AccountSP.getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(QLApi.class)
                .unpinTasks(AccountSP.getAuthorization(), body);

        call.enqueue(new Callback<QLBaseRes>() {
            @Override
            public void onResponse(@NonNull Call<QLBaseRes> call, @NonNull Response<QLBaseRes> response) {
                RequestManager.finishCall(requestId);
                QLBaseRes res = response.body();
                if (res == null) {
                    if (response.code() == 401) {
                        callback.onFailure(ERROR_INVALID_AUTH);
                    } else {
                        callback.onFailure(ERROR_NO_BODY + response.code());
                    }
                } else {
                    if (res.getCode() == 200) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure(res.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<QLBaseRes> call, @NonNull Throwable t) {
                RequestManager.finishCall(requestId);
                if (call.isCanceled()) {
                    return;
                }
                callback.onFailure(t.getLocalizedMessage());
            }
        });

        RequestManager.addCall(call, requestId);

    }

    public static void deleteTasks(@NonNull String requestId, @NonNull List<String> taskIds, @NonNull NetBaseCallback callback) {
        JsonArray jsonArray = new JsonArray();
        for (int i = 0; i < taskIds.size(); i++) {
            jsonArray.add(taskIds.get(i));
        }

        String json = jsonArray.toString();
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
        Call<QLBaseRes> call = new Retrofit.Builder()
                .baseUrl(AccountSP.getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(QLApi.class)
                .deleteTasks(AccountSP.getAuthorization(), body);

        call.enqueue(new Callback<QLBaseRes>() {
            @Override
            public void onResponse(@NonNull Call<QLBaseRes> call, @NonNull Response<QLBaseRes> response) {
                RequestManager.finishCall(requestId);
                QLBaseRes res = response.body();
                if (res == null) {
                    if (response.code() == 401) {
                        callback.onFailure(ERROR_INVALID_AUTH);
                    } else {
                        callback.onFailure(ERROR_NO_BODY + response.code());
                    }
                } else {
                    if (res.getCode() == 200) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure(res.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<QLBaseRes> call, @NonNull Throwable t) {
                RequestManager.finishCall(requestId);
                if (call.isCanceled()) {
                    return;
                }
                callback.onFailure(t.getLocalizedMessage());
            }
        });

        RequestManager.addCall(call, requestId);

    }

    public static void editTask(@NonNull String requestId, @NonNull QLTask QLTask, @NonNull NetEditTaskCallback callback) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("name", QLTask.getName());
        jsonObject.addProperty("_id", QLTask.getId());
        jsonObject.addProperty("command", QLTask.getCommand());
        jsonObject.addProperty("schedule", QLTask.getSchedule());

        String json = jsonObject.toString();

        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
        Call<QLTaskEditRes> call = new Retrofit.Builder()
                .baseUrl(AccountSP.getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(QLApi.class)
                .updateTask(AccountSP.getAuthorization(), body);

        call.enqueue(new Callback<QLTaskEditRes>() {
            @Override
            public void onResponse(Call<QLTaskEditRes> call, Response<QLTaskEditRes> response) {
                RequestManager.finishCall(requestId);
                if (response.body() == null) {
                    if (response.code() == 401) {
                        callback.onFailure(ERROR_INVALID_AUTH);
                    } else {
                        callback.onFailure(ERROR_NO_BODY + response.code());
                    }
                } else {
                    if (response.body().getCode() == 200) {
                        callback.onSuccess(response.body().getData());
                    } else {
                        callback.onFailure(response.body().getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<QLTaskEditRes> call, Throwable t) {
                RequestManager.finishCall(requestId);
                if (call.isCanceled()) {
                    return;
                }
                callback.onFailure(t.getLocalizedMessage());
            }
        });

        RequestManager.addCall(call, requestId);
    }

    public static void addTask(@NonNull String requestId, @NonNull QLTask QLTask, @NonNull NetEditTaskCallback callback) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("name", QLTask.getName());
        jsonObject.addProperty("command", QLTask.getCommand());
        jsonObject.addProperty("schedule", QLTask.getSchedule());

        String json = jsonObject.toString();
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
        Call<QLTaskEditRes> call = new Retrofit.Builder()
                .baseUrl(AccountSP.getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(QLApi.class)
                .addTask(AccountSP.getAuthorization(), body);

        call.enqueue(new Callback<QLTaskEditRes>() {
            @Override
            public void onResponse(Call<QLTaskEditRes> call, Response<QLTaskEditRes> response) {
                RequestManager.finishCall(requestId);
                if (response.body() == null) {
                    if (response.code() == 401) {
                        callback.onFailure(ERROR_INVALID_AUTH);
                    } else {
                        callback.onFailure(ERROR_NO_BODY + response.code());
                    }
                } else {
                    if (response.body().getCode() == 200) {
                        callback.onSuccess(response.body().getData());
                    } else {
                        callback.onFailure(response.body().getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<QLTaskEditRes> call, Throwable t) {
                RequestManager.finishCall(requestId);
                if (call.isCanceled()) {
                    return;
                }
                callback.onFailure(t.getLocalizedMessage());
            }
        });

        RequestManager.addCall(call, requestId);
    }

    public static void getEnvironments(@NonNull String requestId, @NonNull String searchValue, @NonNull NetGetEnvironmentsCallback callback) {
        Call<QLEnvironmentRes> call = new Retrofit.Builder()
                .baseUrl(AccountSP.getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(QLApi.class)
                .getEnvironments(AccountSP.getAuthorization(), searchValue);
        call.enqueue(new Callback<QLEnvironmentRes>() {
            @Override
            public void onResponse(Call<QLEnvironmentRes> call, Response<QLEnvironmentRes> response) {
                RequestManager.finishCall(requestId);
                QLEnvironmentRes environmentRes = response.body();
                if (environmentRes == null) {
                    if (response.code() == 401) {
                        callback.onFailure(ERROR_INVALID_AUTH);
                    } else {
                        callback.onFailure(ERROR_NO_BODY);
                    }
                } else {
                    if (environmentRes.getCode() == 200) {
                        callback.onSuccess(environmentRes);
                    } else {
                        callback.onFailure(environmentRes.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<QLEnvironmentRes> call, Throwable t) {
                RequestManager.finishCall(requestId);
                if (call.isCanceled()) {
                    return;
                }
                callback.onFailure(t.getLocalizedMessage());
            }
        });

        RequestManager.addCall(call, requestId);
    }

    public static void addEnvironment(@NonNull String requestId, @NonNull List<QLEnvironment> environments, @NonNull NetGetEnvironmentsCallback callback) {
        JsonArray jsonArray = new JsonArray();
        JsonObject jsonObject;
        for (QLEnvironment environment : environments) {
            jsonObject = new JsonObject();
            jsonObject.addProperty("name", environment.getName());
            jsonObject.addProperty("remarks", environment.getRemarks());
            jsonObject.addProperty("value", environment.getValue());
            jsonArray.add(jsonObject);
        }
        String json = jsonArray.toString();

        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), json);
        Call<QLEnvironmentRes> call = new Retrofit.Builder()
                .baseUrl(AccountSP.getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(QLApi.class)
                .addEnvironments(AccountSP.getAuthorization(), requestBody);

        call.enqueue(new Callback<QLEnvironmentRes>() {
            @Override
            public void onResponse(Call<QLEnvironmentRes> call, Response<QLEnvironmentRes> response) {
                RequestManager.finishCall(requestId);
                QLEnvironmentRes environmentRes = response.body();
                if (environmentRes == null) {
                    if (response.code() == 401) {
                        callback.onFailure(ERROR_INVALID_AUTH);
                    } else {
                        callback.onFailure(ERROR_NO_BODY);
                    }
                } else {
                    if (environmentRes.getCode() == 200) {
                        callback.onSuccess(environmentRes);
                    } else {
                        callback.onFailure(environmentRes.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<QLEnvironmentRes> call, Throwable t) {
                RequestManager.finishCall(requestId);
                if (call.isCanceled()) {
                    return;
                }
                callback.onFailure(t.getLocalizedMessage());
            }
        });

        RequestManager.addCall(call, requestId);
    }

    public static void updateEnvironment(@NonNull String requestId, @NonNull QLEnvironment environment, @NonNull NetEditEnvCallback callback) {
        JsonObject jsonObject;
        jsonObject = new JsonObject();
        jsonObject.addProperty("name", environment.getName());
        jsonObject.addProperty("remarks", environment.getRemarks());
        jsonObject.addProperty("value", environment.getValue());
        jsonObject.addProperty("_id", environment.get_id());

        String json = jsonObject.toString();
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), json);
        Call<QLEnvEditRes> call = new Retrofit.Builder()
                .baseUrl(AccountSP.getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(QLApi.class)
                .updateEnvironment(AccountSP.getAuthorization(), requestBody);

        call.enqueue(new Callback<QLEnvEditRes>() {
            @Override
            public void onResponse(Call<QLEnvEditRes> call, Response<QLEnvEditRes> response) {
                RequestManager.finishCall(requestId);
                QLEnvEditRes editEnvRes = response.body();
                if (editEnvRes == null) {
                    if (response.code() == 401) {
                        callback.onFailure(ERROR_INVALID_AUTH);
                    } else {
                        callback.onFailure(ERROR_NO_BODY);
                    }
                } else {
                    if (editEnvRes.getCode() == 200) {
                        callback.onSuccess(editEnvRes.getData());
                    } else {
                        callback.onFailure(editEnvRes.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<QLEnvEditRes> call, Throwable t) {
                RequestManager.finishCall(requestId);
                if (call.isCanceled()) {
                    return;
                }
                callback.onFailure(t.getLocalizedMessage());
            }
        });

        RequestManager.addCall(call, requestId);
    }

    public static void deleteEnvironments(@NonNull String requestId, @NonNull List<String> envIds, @NonNull NetBaseCallback callback) {
        JsonArray jsonArray = new JsonArray();
        for (int i = 0; i < envIds.size(); i++) {
            jsonArray.add(envIds.get(i));
        }

        String json = jsonArray.toString();
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
        Call<QLBaseRes> call = new Retrofit.Builder()
                .baseUrl(AccountSP.getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(QLApi.class)
                .deleteEnvironments(AccountSP.getAuthorization(), body);

        call.enqueue(new Callback<QLBaseRes>() {
            @Override
            public void onResponse(Call<QLBaseRes> call, Response<QLBaseRes> response) {
                RequestManager.finishCall(requestId);
                QLBaseRes baseRes = response.body();
                if (baseRes == null) {
                    if (response.code() == 401) {
                        callback.onFailure(ERROR_INVALID_AUTH);
                    } else {
                        callback.onFailure(ERROR_NO_BODY + response.code());
                    }
                } else {
                    if (response.body().getCode() == 200) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure(baseRes.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<QLBaseRes> call, Throwable t) {
                RequestManager.finishCall(requestId);
                if (call.isCanceled()) {
                    return;
                }
                callback.onFailure(t.getLocalizedMessage());
            }
        });

        RequestManager.addCall(call, requestId);

    }

    public static void enableEnvironments(@NonNull String requestId, @NonNull List<String> envIds, @NonNull NetBaseCallback callback) {
        JsonArray jsonArray = new JsonArray();
        for (int i = 0; i < envIds.size(); i++) {
            jsonArray.add(envIds.get(i));
        }

        String json = jsonArray.toString();
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
        Call<QLBaseRes> call = new Retrofit.Builder()
                .baseUrl(AccountSP.getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(QLApi.class)
                .enableEnv(AccountSP.getAuthorization(), body);

        call.enqueue(new Callback<QLBaseRes>() {
            @Override
            public void onResponse(Call<QLBaseRes> call, Response<QLBaseRes> response) {
                RequestManager.finishCall(requestId);
                QLBaseRes baseRes = response.body();
                if (baseRes == null) {
                    if (response.code() == 401) {
                        callback.onFailure(ERROR_INVALID_AUTH);
                    } else {
                        callback.onFailure(ERROR_NO_BODY + response.code());
                    }
                } else {
                    if (response.body().getCode() == 200) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure(baseRes.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<QLBaseRes> call, Throwable t) {
                RequestManager.finishCall(requestId);
                if (call.isCanceled()) {
                    return;
                }
                callback.onFailure(t.getLocalizedMessage());
            }
        });

        RequestManager.addCall(call, requestId);

    }

    public static void disableEnvironments(@NonNull String requestId, @NonNull List<String> envIds, @NonNull NetBaseCallback callback) {
        JsonArray jsonArray = new JsonArray();
        for (int i = 0; i < envIds.size(); i++) {
            jsonArray.add(envIds.get(i));
        }

        String json = jsonArray.toString();
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
        Call<QLBaseRes> call = new Retrofit.Builder()
                .baseUrl(AccountSP.getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(QLApi.class)
                .disableEnv(AccountSP.getAuthorization(), body);

        call.enqueue(new Callback<QLBaseRes>() {
            @Override
            public void onResponse(Call<QLBaseRes> call, Response<QLBaseRes> response) {
                RequestManager.finishCall(requestId);
                QLBaseRes baseRes = response.body();
                if (baseRes == null) {
                    if (response.code() == 401) {
                        callback.onFailure(ERROR_INVALID_AUTH);
                    } else {
                        callback.onFailure(ERROR_NO_BODY + response.code());
                    }
                } else {
                    if (response.body().getCode() == 200) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure(baseRes.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<QLBaseRes> call, Throwable t) {
                RequestManager.finishCall(requestId);
                if (call.isCanceled()) {
                    return;
                }
                callback.onFailure(t.getLocalizedMessage());
            }
        });

        RequestManager.addCall(call, requestId);

    }

    public static void getLogs(@NonNull String requestId, @NonNull NetGetLogsCallback callback) {
        Call<QLLogsRes> call = new Retrofit.Builder()
                .baseUrl(AccountSP.getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(QLApi.class)
                .getLogs(AccountSP.getAuthorization());
        call.enqueue(new Callback<QLLogsRes>() {
            @Override
            public void onResponse(Call<QLLogsRes> call, Response<QLLogsRes> response) {
                RequestManager.finishCall(requestId);
                QLLogsRes logRes = response.body();
                if (logRes == null) {
                    if (response.code() == 401) {
                        callback.onFailure(ERROR_INVALID_AUTH);
                    } else {
                        callback.onFailure(ERROR_NO_BODY);
                    }
                } else {
                    if (logRes.getCode() == 200) {
                        callback.onSuccess(logRes.getDirs());
                    } else {
                        callback.onFailure(logRes.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<QLLogsRes> call, Throwable t) {
                RequestManager.finishCall(requestId);
                if (call.isCanceled()) {
                    return;
                }
                callback.onFailure(t.getLocalizedMessage());
            }
        });

        RequestManager.addCall(call, requestId);
    }

    public static void getLogDetail(@NonNull String requestId, @NonNull String logPath, @NonNull NetTextCallBack callback) {
        Call<QLTextRes> call = new Retrofit.Builder()
                .baseUrl(AccountSP.getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(QLApi.class)
                .getLogDetail(logPath, AccountSP.getAuthorization());

        call.enqueue(new Callback<QLTextRes>() {
            @Override
            public void onResponse(Call<QLTextRes> call, Response<QLTextRes> response) {
                RequestManager.finishCall(requestId);
                QLTextRes textRes = response.body();
                if (textRes == null) {
                    if (response.code() == 401) {
                        callback.onFailure(ERROR_INVALID_AUTH);
                    } else {
                        callback.onFailure(ERROR_NO_BODY);
                    }
                } else {
                    callback.onSuccess(textRes.getData());
                }
            }

            @Override
            public void onFailure(Call<QLTextRes> call, Throwable t) {
                RequestManager.finishCall(requestId);
                if (call.isCanceled()) {
                    return;
                }
                callback.onFailure(t.getLocalizedMessage());
            }
        });
        RequestManager.addCall(call, requestId);
    }

    public static void getConfigDetail(@NonNull String requestId, @NonNull NetConfigCallback callback) {
        Call<QLTextRes> call = new Retrofit.Builder()
                .baseUrl(AccountSP.getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(QLApi.class)
                .getConfig(AccountSP.getAuthorization());

        call.enqueue(new Callback<QLTextRes>() {
            @Override
            public void onResponse(Call<QLTextRes> call, Response<QLTextRes> response) {
                RequestManager.finishCall(requestId);
                QLTextRes res = response.body();
                if (res == null) {
                    if (response.code() == 401) {
                        callback.onFailure(ERROR_INVALID_AUTH);
                    } else {
                        callback.onFailure(ERROR_NO_BODY);
                    }
                } else {
                    if (res.getCode() == 200) {
                        callback.onSuccess(res.getData());
                    } else {
                        callback.onFailure(res.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<QLTextRes> call, Throwable t) {
                RequestManager.finishCall(requestId);
                if (call.isCanceled()) {
                    return;
                }
                callback.onFailure(t.getLocalizedMessage());
            }
        });
        RequestManager.addCall(call, requestId);
    }

    public static void saveConfig(@NonNull String requestId, @NonNull String content, @NonNull NetBaseCallback callback) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("content", content);
        jsonObject.addProperty("name", "config.sh");

        String json = jsonObject.toString();
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);

        Call<QLBaseRes> call = new Retrofit.Builder()
                .baseUrl(AccountSP.getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(QLApi.class)
                .updateConfig(AccountSP.getAuthorization(), body);

        call.enqueue(new Callback<QLBaseRes>() {
            @Override
            public void onResponse(Call<QLBaseRes> call, Response<QLBaseRes> response) {
                RequestManager.finishCall(requestId);
                QLBaseRes res = response.body();
                if (res == null) {
                    if (response.code() == 401) {
                        callback.onFailure(ERROR_INVALID_AUTH);
                    } else {
                        callback.onFailure(ERROR_NO_BODY);
                    }
                } else {
                    if (res.getCode() == 200) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure(res.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<QLBaseRes> call, Throwable t) {
                RequestManager.finishCall(requestId);
                if (call.isCanceled()) {
                    return;
                }
                callback.onFailure(t.getLocalizedMessage());
            }
        });

        RequestManager.addCall(call, requestId);
    }

    public static void getScripts(@NonNull String requestId, @NonNull NetGetScriptsCallback callback) {
        Call<QLScriptsRes> call = new Retrofit.Builder()
                .baseUrl(AccountSP.getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(QLApi.class)
                .getScripts(AccountSP.getAuthorization());
        call.enqueue(new Callback<QLScriptsRes>() {
            @Override
            public void onResponse(Call<QLScriptsRes> call, Response<QLScriptsRes> response) {
                RequestManager.finishCall(requestId);
                QLScriptsRes res = response.body();
                if (res == null) {
                    if (response.code() == 401) {
                        callback.onFailure(ERROR_INVALID_AUTH);
                    } else {
                        callback.onFailure(ERROR_NO_BODY);
                    }
                } else {
                    if (res.getCode() == 200) {
                        callback.onSuccess(res.getData());
                    } else {
                        callback.onFailure(res.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<QLScriptsRes> call, Throwable t) {
                RequestManager.finishCall(requestId);
                if (call.isCanceled()) {
                    return;
                }
                callback.onFailure(t.getLocalizedMessage());
            }
        });

        RequestManager.addCall(call, requestId);
    }

    public static void getScriptDetail(@NonNull String requestId, @NonNull String scriptPath, @NonNull NetTextCallBack callback) {
        Call<QLTextRes> call = new Retrofit.Builder()
                .baseUrl(AccountSP.getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(QLApi.class)
                .getScriptDetail(scriptPath, AccountSP.getAuthorization());

        call.enqueue(new Callback<QLTextRes>() {
            @Override
            public void onResponse(Call<QLTextRes> call, Response<QLTextRes> response) {
                RequestManager.finishCall(requestId);
                QLTextRes res = response.body();
                if (res == null) {
                    if (response.code() == 401) {
                        callback.onFailure(ERROR_INVALID_AUTH);
                    } else {
                        callback.onFailure(ERROR_NO_BODY);
                    }
                } else {
                    if (res.getCode() == 200) {
                        callback.onSuccess(res.getData());
                    } else {
                        callback.onFailure(res.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<QLTextRes> call, Throwable t) {
                RequestManager.finishCall(requestId);
                if (call.isCanceled()) {
                    return;
                }
                callback.onFailure(t.getLocalizedMessage());
            }
        });
        RequestManager.addCall(call, requestId);
    }

    public static void saveScript(@NonNull String requestId, @NonNull String content, @NonNull String filename, String path, @NonNull NetBaseCallback callback) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("content", content);
        jsonObject.addProperty("filename", filename);
        jsonObject.addProperty("path", path == null ? "" : path);

        String json = jsonObject.toString();
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);

        Call<QLBaseRes> call = new Retrofit.Builder()
                .baseUrl(AccountSP.getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(QLApi.class)
                .updateScript(AccountSP.getAuthorization(), body);

        call.enqueue(new Callback<QLBaseRes>() {
            @Override
            public void onResponse(Call<QLBaseRes> call, Response<QLBaseRes> response) {
                RequestManager.finishCall(requestId);
                if (response.body() == null) {
                    if (response.code() == 401) {
                        callback.onFailure(ERROR_INVALID_AUTH);
                    } else {
                        callback.onFailure(ERROR_NO_BODY);
                    }
                } else {
                    callback.onSuccess();
                }
            }

            @Override
            public void onFailure(Call<QLBaseRes> call, Throwable t) {
                RequestManager.finishCall(requestId);
                if (call.isCanceled()) {
                    return;
                }
                callback.onFailure(t.getLocalizedMessage());
            }
        });

        RequestManager.addCall(call, requestId);
    }

    public static void getDependencies(@NonNull String requestId, String searchValue, String type, @NonNull NetGetDependenciesCallback callback) {
        Call<QLDependenciesRes> call = new Retrofit.Builder()
                .baseUrl(AccountSP.getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(QLApi.class)
                .getDependencies(AccountSP.getAuthorization(), searchValue, type);

        call.enqueue(new Callback<QLDependenciesRes>() {
            @Override
            public void onResponse(Call<QLDependenciesRes> call, Response<QLDependenciesRes> response) {
                RequestManager.finishCall(requestId);
                if (response.body() == null) {
                    if (response.code() == 401) {
                        callback.onFailure(ERROR_INVALID_AUTH);
                    } else {
                        callback.onFailure(ERROR_NO_BODY);
                    }
                } else {
                    callback.onSuccess(response.body());
                }
            }

            @Override
            public void onFailure(Call<QLDependenciesRes> call, Throwable t) {
                RequestManager.finishCall(requestId);
                if (call.isCanceled()) {
                    return;
                }
                callback.onFailure(t.getLocalizedMessage());
            }
        });
        RequestManager.addCall(call, requestId);

    }

    public static void addDependencies(@NonNull String requestId, @NonNull List<QLDependence> dependencies, @NonNull NetBaseCallback callback) {
        JsonArray jsonArray = new JsonArray();
        JsonObject jsonObject;
        for (QLDependence QLDependence : dependencies) {
            jsonObject = new JsonObject();
            jsonObject.addProperty("name", QLDependence.getName());
            jsonObject.addProperty("type", QLDependence.getType());
            jsonArray.add(jsonObject);
        }

        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonArray.toString());
        Call<QLBaseRes> call = new Retrofit.Builder()
                .baseUrl(AccountSP.getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(QLApi.class)
                .addDependencies(AccountSP.getAuthorization(), requestBody);

        call.enqueue(new Callback<QLBaseRes>() {
            @Override
            public void onResponse(Call<QLBaseRes> call, Response<QLBaseRes> response) {
                RequestManager.finishCall(requestId);
                QLBaseRes baseRes = response.body();
                if (baseRes == null) {
                    if (response.code() == 401) {
                        callback.onFailure(ERROR_INVALID_AUTH);
                    } else {
                        callback.onFailure(ERROR_NO_BODY);
                    }
                } else {
                    if (baseRes.getCode() == 200) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure(baseRes.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<QLBaseRes> call, Throwable t) {
                RequestManager.finishCall(requestId);
                if (call.isCanceled()) {
                    return;
                }
                callback.onFailure(t.getLocalizedMessage());
            }
        });

        RequestManager.addCall(call, requestId);
    }

    public static void deleteDependencies(@NonNull String requestId, @NonNull List<String> ids, @NonNull NetBaseCallback callback) {
        JsonArray jsonArray = new JsonArray();
        for (String id : ids) {
            jsonArray.add(id);
        }
        String json = jsonArray.toString();

        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), json);
        Call<QLBaseRes> call = new Retrofit.Builder()
                .baseUrl(AccountSP.getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(QLApi.class)
                .deleteDependencies(AccountSP.getAuthorization(), requestBody);

        call.enqueue(new Callback<QLBaseRes>() {
            @Override
            public void onResponse(Call<QLBaseRes> call, Response<QLBaseRes> response) {
                RequestManager.finishCall(requestId);
                QLBaseRes baseRes = response.body();
                if (baseRes == null) {
                    if (response.code() == 401) {
                        callback.onFailure(ERROR_INVALID_AUTH);
                    } else {
                        callback.onFailure(ERROR_NO_BODY);
                    }
                } else {
                    if (baseRes.getCode() == 200) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure(baseRes.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<QLBaseRes> call, Throwable t) {
                RequestManager.finishCall(requestId);
                if (call.isCanceled()) {
                    return;
                }
                callback.onFailure(t.getLocalizedMessage());
            }
        });

        RequestManager.addCall(call, requestId);
    }

    public static void getDependence(@NonNull String requestId, @NonNull String path, @NonNull NetGetDependenceCallback callback) {
        Call<QLDependenceRes> call = new Retrofit.Builder()
                .baseUrl(AccountSP.getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(QLApi.class)
                .getDependence(path, AccountSP.getAuthorization());

        call.enqueue(new Callback<QLDependenceRes>() {
            @Override
            public void onResponse(Call<QLDependenceRes> call, Response<QLDependenceRes> response) {
                RequestManager.finishCall(requestId);
                QLDependenceRes res = response.body();
                if (res == null) {
                    if (response.code() == 401) {
                        callback.onFailure(ERROR_INVALID_AUTH);
                    } else {
                        callback.onFailure(ERROR_NO_BODY);
                    }
                } else {
                    if (res.getCode() == 200) {
                        callback.onSuccess(res.getData());
                    } else {
                        callback.onFailure(res.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<QLDependenceRes> call, Throwable t) {
                RequestManager.finishCall(requestId);
                if (call.isCanceled()) {
                    return;
                }
                callback.onFailure(t.getLocalizedMessage());
            }
        });
        RequestManager.addCall(call, requestId);
    }

    public static void reinstallDependencies(@NonNull String requestId, @NonNull List<String> ids, @NonNull NetBaseCallback callback) {
        JsonArray jsonArray = new JsonArray();
        for (String id : ids) {
            jsonArray.add(id);
        }

        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonArray.toString());
        Call<QLBaseRes> call = new Retrofit.Builder()
                .baseUrl(AccountSP.getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(QLApi.class)
                .reinstallDependencies(AccountSP.getAuthorization(), requestBody);

        call.enqueue(new Callback<QLBaseRes>() {
            @Override
            public void onResponse(Call<QLBaseRes> call, Response<QLBaseRes> response) {
                RequestManager.finishCall(requestId);
                QLBaseRes baseRes = response.body();
                if (baseRes == null) {
                    if (response.code() == 401) {
                        callback.onFailure(ERROR_INVALID_AUTH);
                    } else {
                        callback.onFailure(ERROR_NO_BODY);
                    }
                } else {
                    if (baseRes.getCode() == 200) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure(baseRes.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<QLBaseRes> call, Throwable t) {
                RequestManager.finishCall(requestId);
                if (call.isCanceled()) {
                    return;
                }
                callback.onFailure(t.getLocalizedMessage());
            }
        });

        RequestManager.addCall(call, requestId);
    }

    public static void getLoginLogs(@NonNull String requestId, @NonNull NetGetLoginLogsCallback callback) {
        Call<QLLoginLogsRes> call = new Retrofit.Builder()
                .baseUrl(Objects.requireNonNull(AccountSP.getCurrentAccount()).getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(QLApi.class)
                .getLoginLogs(AccountSP.getAuthorization());

        call.enqueue(new Callback<QLLoginLogsRes>() {
            @Override
            public void onResponse(Call<QLLoginLogsRes> call, Response<QLLoginLogsRes> response) {
                RequestManager.finishCall(requestId);
                QLLoginLogsRes loginLogRes = response.body();
                if (loginLogRes == null) {
                    if (response.code() == 401) {
                        callback.onFailure(ERROR_INVALID_AUTH);
                    } else {
                        callback.onFailure(ERROR_NO_BODY);
                    }
                } else {
                    if (loginLogRes.getCode() == 200) {
                        callback.onSuccess(loginLogRes.getData());
                    } else {
                        callback.onFailure(loginLogRes.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<QLLoginLogsRes> call, Throwable t) {
                RequestManager.finishCall(requestId);
                if (call.isCanceled()) {
                    return;
                }
                callback.onFailure(t.getLocalizedMessage());
            }
        });

        RequestManager.addCall(call, requestId);
    }

    public static void getLogRemove(@NonNull String requestId, @NonNull NetGetLogRemoveCallback callback) {
        Call<QLLogRemoveRes> call = new Retrofit.Builder()
                .baseUrl(Objects.requireNonNull(AccountSP.getCurrentAccount()).getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(QLApi.class)
                .getLogRemove(AccountSP.getAuthorization());

        call.enqueue(new Callback<QLLogRemoveRes>() {
            @Override
            public void onResponse(Call<QLLogRemoveRes> call, Response<QLLogRemoveRes> response) {
                RequestManager.finishCall(requestId);
                QLLogRemoveRes logRemoveRes = response.body();
                if (logRemoveRes == null) {
                    if (response.code() == 401) {
                        callback.onFailure(ERROR_INVALID_AUTH);
                    } else {
                        callback.onFailure(ERROR_NO_BODY);
                    }
                } else {
                    if (logRemoveRes.getCode() == 200) {
                        callback.onSuccess(logRemoveRes.getData().getFrequency());
                    } else {
                        callback.onFailure(logRemoveRes.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<QLLogRemoveRes> call, Throwable t) {
                RequestManager.finishCall(requestId);
                if (call.isCanceled()) {
                    return;
                }
                callback.onFailure(t.getLocalizedMessage());
            }
        });

        RequestManager.addCall(call, requestId);
    }

    public static void updateLogRemove(@NonNull String requestId, int frequency, @NonNull NetBaseCallback callback) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("frequency", frequency);

        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonObject.toString());
        Call<QLBaseRes> call = new Retrofit.Builder()
                .baseUrl(AccountSP.getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(QLApi.class)
                .updateLogRemove(AccountSP.getAuthorization(), requestBody);

        call.enqueue(new Callback<QLBaseRes>() {
            @Override
            public void onResponse(Call<QLBaseRes> call, Response<QLBaseRes> response) {
                RequestManager.finishCall(requestId);
                QLBaseRes baseRes = response.body();
                if (baseRes == null) {
                    if (response.code() == 401) {
                        callback.onFailure(ERROR_INVALID_AUTH);
                    } else {
                        callback.onFailure(ERROR_NO_BODY);
                    }
                } else {
                    if (baseRes.getCode() == 200) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure(baseRes.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<QLBaseRes> call, Throwable t) {
                RequestManager.finishCall(requestId);
                if (call.isCanceled()) {
                    return;
                }
                callback.onFailure(t.getLocalizedMessage());
            }
        });

        RequestManager.addCall(call, requestId);

    }

    public static void updateUser(@NonNull String requestId, Account account, @NonNull NetBaseCallback callback) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("username", account.getUsername());
        jsonObject.addProperty("password", account.getPassword());

        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonObject.toString());
        Call<QLBaseRes> call = new Retrofit.Builder()
                .baseUrl(AccountSP.getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(QLApi.class)
                .updateUser(AccountSP.getAuthorization(), requestBody);

        call.enqueue(new Callback<QLBaseRes>() {
            @Override
            public void onResponse(Call<QLBaseRes> call, Response<QLBaseRes> response) {
                RequestManager.finishCall(requestId);
                QLBaseRes baseRes = response.body();
                if (baseRes == null) {
                    if (response.code() == 401) {
                        callback.onFailure(ERROR_INVALID_AUTH);
                    } else {
                        callback.onFailure(ERROR_NO_BODY);
                    }
                } else {
                    if (baseRes.getCode() == 200) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure(baseRes.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<QLBaseRes> call, Throwable t) {
                RequestManager.finishCall(requestId);
                if (call.isCanceled()) {
                    return;
                }
                callback.onFailure(t.getLocalizedMessage());
            }
        });

        RequestManager.addCall(call, requestId);

    }


    public interface NetBaseCallback {
        void onSuccess();

        void onFailure(String msg);
    }

    public interface NetTextCallBack {
        void onSuccess(String content);

        void onFailure(String msg);
    }

    public interface NetSystemCallback {
        void onSuccess(QLSystemRes systemRes);

        void onFailure(String msg);

    }

    public interface NetConfigCallback {
        void onSuccess(String content);

        void onFailure(String msg);
    }

    public interface NetLoginCallback {
        void onSuccess(Account account);

        void onFailure(String msg);
    }

    public interface NetRunTaskCallback {
        void onSuccess(String msg);

        void onFailure(String msg);
    }

    public interface NetEditTaskCallback {
        void onSuccess(QLTask QLTask);

        void onFailure(String msg);
    }

    public interface NetGetTasksCallback {
        void onSuccess(QLTasksRes data);

        void onFailure(String msg);
    }

    public interface NetGetScriptsCallback {
        void onSuccess(List<QLScript> QLScripts);

        void onFailure(String msg);
    }

    public interface NetGetLogsCallback {
        void onSuccess(List<QLLog> QLLogs);

        void onFailure(String msg);
    }

    public interface NetGetEnvironmentsCallback {
        void onSuccess(QLEnvironmentRes res);

        void onFailure(String msg);
    }

    public interface NetGetDependenciesCallback {
        void onSuccess(QLDependenciesRes res);

        void onFailure(String msg);
    }

    public interface NetGetDependenceCallback {
        void onSuccess(QLDependence dependence);

        void onFailure(String msg);
    }

    public interface NetEditEnvCallback {
        void onSuccess(QLEnvironment QLEnvironment);

        void onFailure(String msg);
    }

    public interface NetGetLoginLogsCallback {
        void onSuccess(List<QLLoginLog> logs);

        void onFailure(String msg);
    }

    public interface NetGetLogRemoveCallback {
        void onSuccess(int frequency);

        void onFailure(String msg);
    }
}
