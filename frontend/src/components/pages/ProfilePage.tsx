import React, {useEffect, useMemo, useState} from 'react'
import {Button, Group, PasswordInput, TextInput, Title} from "@mantine/core";
import {pipe, prop} from "ramda";
import {IRegistrationRequest, IUser} from "../../model/model";
import axios from "axios";
import {errorHandler, responseToNotification} from "../utils/apiUtils";
import {IApiResponse} from "../../model/rests";
import {useForm, zodResolver} from "@mantine/form";
import {z} from "zod";
import {getUser} from "../utils/userUtils";

export const ProfilePage: React.FC = () => {
    const user: IUser | null = getUser()
    const [response, setResponse] = useState<IApiResponse<boolean>>()
    const [isLoading, setIsLoading] = useState(false)

    const onSubmit = (password: string) => {
        setIsLoading(true)
        axios.put(`/api/user/me`, {password})
            .then(pipe(prop('data'), setResponse))
            .catch(pipe(errorHandler, setResponse))
            .finally(() => setIsLoading(false))
    }

    useEffect(() => {
        if (response?.success) {
            const timer = setTimeout(() => {
                document.location = '/';
            }, 1000);
            return () => clearTimeout(timer);
        }
    }, [response])

    const form = useForm({
        initialValues: {
            password: '',
            passwordCopy: '',
        },
        schema: zodResolver(z.object({
            password: z.string().trim().min(8, {message: 'Пароль не может быть короче 8 символов'}),
            passwordCopy: z.string().trim()
        }).superRefine((data, ctx) => {
                if (data.passwordCopy !== data.password)
                    ctx.addIssue({
                        code: z.ZodIssueCode.custom,
                        path: ["passwordCopy"],
                        message: "Пароли должны совпадать"
                    })
            }
        )),
    });

    return <>
        <Title>Профиль</Title>
        {!!response && responseToNotification(response)}
        <form onSubmit={form.onSubmit((values) => onSubmit(values.password))}>
            <TextInput
                label='E-mail'
                value={user?.email}
                required
                disabled
            />
            <PasswordInput
                label='Новый пароль'
                {...form.getInputProps('password')}
                required
            />
            <PasswordInput
                label='Повторите пароль'
                {...form.getInputProps('passwordCopy')}
                required
            />
            <Group position='left' mt='md'>
                <Button type='submit' disabled={response?.success || isLoading}>Отправить</Button>
            </Group>
        </form>
    </>
}

