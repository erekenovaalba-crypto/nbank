package requests.skelethon.interfaces;
import models.BaseModel;

public interface CrudEndpointInterface {
    // POST без тела, например создание счёта
    Object post();
    Object post(BaseModel model);
    // GET без параметров: список (пользователи) или единственный ресурс (профиль)
    Object get();
    // GET с id в пути, например /accounts/{accountId}/transactions
    Object get(long id);
    // PUT без id, например обновление профиля
    Object update(BaseModel model);
    Object update(long id, BaseModel model);
    Object delete(long id);
}
