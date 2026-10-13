package nestoredidev.common.application;

/** RN-G06: el usuario auditado siempre sale del JWT, nunca del frontend. */
public interface CurrentUserPort {
    CurrentUser get();
}
