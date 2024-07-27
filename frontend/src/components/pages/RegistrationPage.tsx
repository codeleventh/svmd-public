import React, {useEffect, useMemo, useState} from 'react'
import {Button, Group, PasswordInput, TextInput, Title} from "@mantine/core";
import {pipe, prop} from "ramda";
import {IRegistrationRequest} from "../../model/model";
import axios from "axios";
import {errorHandler, responseToNotification} from "../utils/apiUtils";
import {IApiResponse} from "../../model/rests";
import {useForm, zodResolver} from "@mantine/form";
import {z} from "zod";

export const RegistrationPage: React.FC = () => {
    const [response, setResponse] = useState<IApiResponse<boolean>>()
    const [isLoading, setIsLoading] = useState(false)

    const onSubmit = (registrationRequest: IRegistrationRequest) => {
        setIsLoading(true)
        axios.post(`/api/user`, registrationRequest)
            .then(pipe(prop('data'), setResponse))
            .catch(pipe(errorHandler, setResponse))
            .finally(() => setIsLoading(false))
    }

    const form = useForm({
        initialValues: {
            email: '',
            password: '',
            invite: '',
        },
        schema: zodResolver(z.object({
            email: z.string().email('Введите корректный адрес почты'),
            password: z.string().min(8, {message: 'Пароль не может быть короче 8 символов'}),
            invite: z.string().uuid('Неправильный код приглашения')
        })),
    });

    useEffect(() => {
        if (response?.success) {
            const timer = setTimeout(() => {
                document.location = '/';
                localStorage.setItem('user', JSON.stringify({email: form.getInputProps('email').value}));
            }, 1000);
            return () => clearTimeout(timer);
        }
    }, [response])

    return <>
        <Title>Регистрация</Title>
        {!!response && responseToNotification(response)}
        <form onSubmit={form.onSubmit((values) => onSubmit(values))}>
            <TextInput
                label='E-mail'
                {...form.getInputProps('email')}
                required
            />
            <PasswordInput
                label='Пароль'
                {...form.getInputProps('password')}
                required
            />
            <TextInput
                label='Код приглашения'
                {...form.getInputProps('invite')}
                required
            />

            <Group position='left' mt='md'>
                <Button type='submit' disabled={response?.success || isLoading}>Отправить</Button>
            </Group>
        </form>
    </>
}

